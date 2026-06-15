process.stdout.setDefaultEncoding('utf8');
process.stderr.setDateDefaultEncoding && process.stderr.setDefaultEncoding('utf8');

const { app, BrowserWindow, dialog, Tray, Menu, ipcMain } = require('electron');
const { exec } = require('child_process');
const path = require('path');

const logger = {
    warn: (...args) => console.warn('[WARN]', ...args),
    error: (...args) => console.error('[ERROR]', ...args)
};

const mode = process.env.npm_lifecycle_event;

let mainWindow;
let loadingWindow;
let tray;
let isQuitting = false;

// 1. 创建 Loading 窗口
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

    loadingWindow.loadFile(loadingPath).catch(err => {
        logger.error(err, '无法加载 loading 页面:');
    });

    loadingWindow.once('ready-to-show', () => {
        loadingWindow.show();
    });
};

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
    app.quit();
} else {
    app.on('second-instance', () => {
        if (mainWindow) {
            if (mainWindow.isMinimized()) mainWindow.restore();
            mainWindow.show();
            mainWindow.focus();
        }
    });

    // 2. 创建主窗口
    function createWindow() {
        const iconPath = app.isPackaged
            ? path.join(process.resourcesPath, 'icon.ico')
            : path.join(__dirname, 'icon.ico');

        mainWindow = new BrowserWindow({
            width: 800,
            height: 600,
            autoHideMenuBar: true,
            show: false,          // 初始隐藏
            opacity: 0,           // 初始透明度为 0，用于后面的渐入效果
            icon: iconPath,
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false,
                enableRemoteModule: true,
                autofill: false,
                webSecurity: false
            }
        });

        if (mode === 'electron:dev') {
            mainWindow.loadURL('http://localhost:5199');
        } else {
            const indexPath = path.join(__dirname, 'dist', 'index.html');
            mainWindow.loadFile(indexPath).catch(err => {
                logger.error(err, '加载本地文件失败');
                dialog.showErrorBox('加载错误', `无法加载 index.html: ${err.message}`);
            });
        }

        // 当主窗口 DOM 准备好后，不立刻显示，等待 3 秒定时器控制
        mainWindow.once('ready-to-show', () => {
            logger.warn('主窗口后台加载完毕，等待 3 秒倒计时...');
        });

        mainWindow.on('close', (event) => {
            if (!isQuitting) {
                event.preventDefault();
                const choice = dialog.showMessageBoxSync(mainWindow, {
                    type: 'warning',
                    buttons: ['最小化到托盘', '彻底退出', '取消'],
                    title: '确认关闭',
                    message: '您想如何处理RFID智能柜操作系统？'
                });
                if (choice === 0) {
                    mainWindow.hide();
                    createTray();
                } else if (choice === 1) {
                    isQuitting = true;
                    app.quit();
                }
            }
        });

        mainWindow.on('closed', () => {
            mainWindow = null;
        });
    }

    // 3. 核心：平滑渐入主窗口的函数
    function fadeInMainWindow() {
        if (!mainWindow) return;

        mainWindow.maximize();
        mainWindow.show();
        
        let opacity = 0;
        const animationInterval = setInterval(() => {
            if (opacity >= 1) {
                clearInterval(animationInterval);
                mainWindow.focus();
            } else {
                opacity += 0.05; // 每次增加 0.05 透明度
                mainWindow.setOpacity(opacity);
            }
        }, 16); // 约 60 帧每秒的刷新率，完成渐入大约需要 300 毫秒
    }

    // 系统辅助功能
    function openOnScreenKeyboard() {
        const oskPath = 'C:\\Windows\\System32\\osk.exe';
        exec(`"${oskPath}"`);
    }

    ipcMain.on('close-keyboard', () => {
        exec('taskkill /IM osk.exe /F');
    });

    ipcMain.on('open-keyboard', () => {
        openOnScreenKeyboard();
    });

    // 托盘管理
    function createTray() {
        if (tray) return;
        const isDev = !app.isPackaged;
        const iconPath = isDev
            ? path.join(__dirname, 'icon.jpg')
            : path.join(process.resourcesPath, 'icon.jpg');

        tray = new Tray(iconPath);
        const contextMenu = Menu.buildFromTemplate([
            { label: '显示应用', click: () => mainWindow.show() },
            {
                label: '退出应用',
                click: () => {
                    isQuitting = true;
                    app.quit();
                }
            }
        ]);

        tray.setToolTip('智能RFID柜');
        tray.setContextMenu(contextMenu);
        tray.on('click', () => mainWindow.show());
    }

    // 4. 生命周期控制
    app.whenReady().then(() => {
        logger.warn('正在启动程序...');
        
        // 先显示加载窗口
        createLoadingWindow();
        
        // 预先创建主窗口（在后台静默加载渲染 dist 资源）
        createWindow();

        // 强制等待 3 秒（3000毫秒）
        setTimeout(() => {
            // 渐入显示主窗口
            fadeInMainWindow();

            // 关闭并销毁 loading 窗口
            if (loadingWindow && !loadingWindow.isDestroyed()) {
                loadingWindow.close();
                loadingWindow = null;
            }
            logger.warn('程序成功进入主界面');
        }, 3000);

        app.on('activate', () => {
            if (mainWindow) mainWindow.show();
            else createWindow();
        });
    });

    app.on('before-quit', () => {
        isQuitting = true;
        if (tray) {
            tray.destroy();
            tray = null;
        }
    });

    app.on('window-all-closed', () => {
        if (process.platform !== 'darwin') {
            app.quit();
        }
    });

    process.on('uncaughtException', (err) => {
        logger.error(err, '未捕获的异常');
    });
    process.on('unhandledRejection', (reason, promise) => {
        logger.error(reason, '未处理的 Promise 拒绝');
    });
}