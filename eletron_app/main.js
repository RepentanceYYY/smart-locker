process.stdout.setDefaultEncoding('utf8');
process.stderr.setDateDefaultEncoding && process.stderr.setDefaultEncoding('utf8');

const { app, BrowserWindow, dialog, Tray, Menu, ipcMain } = require('electron');
const { exec, spawn } = require('child_process');
const path = require('path');
const http = require('http'); // HTTP 探测

const ENABLE_JAR = true; // true=启动jar+探测 / false=纯前端3秒进入

const logger = {
    info: (...args) => console.log('[INFO]', ...args),
    warn: (...args) => console.warn('[WARN]', ...args),
    error: (...args) => console.error('[ERROR]', ...args)
};

let mainWindow;
let loadingWindow;
let tray;
let isQuitting = false;
let javaProcess = null;
let httpCheckTimer = null;

// 启动 Jar
function startJavaServer() {
    const resourcePath = app.isPackaged
        ? path.join(process.resourcesPath, 'extraResources')
        : path.join(app.getAppPath(), 'extraResources');

    const javaBin = path.join(resourcePath, 'jdk-17.0.12', 'bin', 'java.exe');
    const jarPath = path.join(resourcePath, 'server.jar');

    logger.info(`正在启动 Java 子进程...`);

    javaProcess = spawn(javaBin, [
        '-Dfile.encoding=UTF-8', 
        '-Dsun.stdout.encoding=UTF-8', 
        '-jar', 
        jarPath
    ], {
        cwd: resourcePath
    });

    javaProcess.stdout.on('data', (data) => {
        console.log(`[Java Stdout]: ${data.toString().trim()}`);
    });

    javaProcess.stderr.on('data', (data) => {
        console.error(`[Java Stderr]: ${data.toString().trim()}`);
    });

    javaProcess.on('close', (code) => {
        logger.warn(`Java 服务已退出: ${code}`);
        javaProcess = null;
    });
}

// HTTP 探测
function connectAndCheckBackend() {
    const targetUrl = 'http://localhost:8080/api/systemConfig';

    const req = http.get(targetUrl, (res) => {
        logger.info(`后端就绪: ${res.statusCode}`);

        if (httpCheckTimer) {
            clearInterval(httpCheckTimer);
            httpCheckTimer = null;
        }

        enterMainInterface();
    });

    req.setTimeout(1000);

    req.on('error', (err) => {
        logger.warn(`未就绪: ${err.message}`);
    });

    req.on('timeout', () => req.destroy());
}

function startBackendPolling() {
    connectAndCheckBackend();
    httpCheckTimer = setInterval(connectAndCheckBackend, 1500);
}

// 统一进入主界面
function enterMainInterface() {
    if (!mainWindow) {
        createWindow();
    }

    fadeInMainWindow();

    if (loadingWindow && !loadingWindow.isDestroyed()) {
        loadingWindow.close();
        loadingWindow = null;
    }

    logger.warn('进入主界面');
}

// 强制延迟进入（无后端模式）
function enterMainInterfaceAfterDelay() {
    setTimeout(() => {
        enterMainInterface();
    }, 3000);
}

// Loading
const createLoadingWindow = () => {
    loadingWindow = new BrowserWindow({
        width: 400,
        height: 300,
        frame: false,
        transparent: true,
        alwaysOnTop: true,
        show: false,
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true
        }
    });

    const loadingPath = path.join(app.getAppPath(), 'loading.html');

    loadingWindow.loadFile(loadingPath);

    loadingWindow.once('ready-to-show', () => {
        loadingWindow.show();
    });
};

// 主窗口
function createWindow() {
    const iconPath = app.isPackaged
        ? path.join(process.resourcesPath, 'icon.ico')
        : path.join(__dirname, 'icon.ico');

    mainWindow = new BrowserWindow({
        width: 800,
        height: 600,
        autoHideMenuBar: true,
        show: false,
        opacity: 0,
        icon: iconPath,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false
        }
    });

    const indexPath = path.join(__dirname, 'dist', 'index.html');

    mainWindow.loadFile(indexPath);

    mainWindow.once('ready-to-show', () => {
        logger.warn('主窗口资源加载完成');
    });
}

function fadeInMainWindow() {
    if (!mainWindow) return;

    mainWindow.maximize();
    mainWindow.show();

    let opacity = 0;
    const timer = setInterval(() => {
        if (opacity >= 1) {
            clearInterval(timer);
            mainWindow.focus();
        } else {
            opacity += 0.05;
            mainWindow.setOpacity(opacity);
        }
    }, 16);
}

// 生命周期
const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
    app.quit();
} else {

    app.whenReady().then(() => {

        logger.warn('启动中...');

        createLoadingWindow();

        if (ENABLE_JAR) {
            // 启动后端
            startJavaServer();
            startBackendPolling();
        } else {
            // 不启动后端，3秒后直接进
            enterMainInterfaceAfterDelay();
        }

        app.on('activate', () => {
            if (mainWindow) mainWindow.show();
        });
    });

    app.on('before-quit', () => {
        isQuitting = true;

        if (httpCheckTimer) clearInterval(httpCheckTimer);

        if (javaProcess) {
            javaProcess.kill('SIGKILL');
            javaProcess = null;
        }

        if (tray) tray.destroy();
    });

    app.on('window-all-closed', () => {
        if (process.platform !== 'darwin') app.quit();
    });
}