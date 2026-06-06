// utils/exportUtils.ts
// 注意：不要使用 import QRCode from 'qrcode'，改用以下方式

// 浏览器环境下的二维码生成库
import QRCode from 'qrcode'
import { saveAs } from 'file-saver'
import * as XLSX from 'xlsx'

// 注意：docx 在浏览器中可能有问题，我们改用其他方式生成 Word 文档
// 这里使用 HTML 转 Word 的方式更可靠

// 格口数据类型
export interface CellExportData {
    cabinetId: number
    cabinetName: string
    slotNumber: number
    toolName: string
    qrcodeContent: string
    qrcodeDataUrl?: string
}

// 获取所有格口数据（从柜子数据中提取）- 只生成 DataURL，不生成 Buffer
export async function collectCellData(cabinets: any[]): Promise<CellExportData[]> {
    const cellData: CellExportData[] = []

    for (const cabinet of cabinets) {
        for (const row of cabinet.rows) {
            for (const cell of row.cells) {
                if (cell.type === 'cell' && cell.qrcodeContent) {
                    try {
                        // 生成二维码 DataURL（用于 HTML 和 Word）
                        const qrcodeDataUrl = await QRCode.toDataURL(cell.qrcodeContent, {
                            width: 150,
                            margin: 1,
                            color: {
                                dark: '#000000',
                                light: '#ffffff'
                            },
                            errorCorrectionLevel: 'H'
                        })

                        cellData.push({
                            cabinetId: cabinet.id,
                            cabinetName: cabinet.title,
                            slotNumber: cell.number,
                            toolName: cell.toolName || '',
                            qrcodeContent: cell.qrcodeContent,
                            qrcodeDataUrl: qrcodeDataUrl
                        })
                    } catch (qrErr) {
                        console.error('生成二维码失败:', qrErr, '内容:', cell.qrcodeContent)
                        cellData.push({
                            cabinetId: cabinet.id,
                            cabinetName: cabinet.title,
                            slotNumber: cell.number,
                            toolName: cell.toolName || '',
                            qrcodeContent: cell.qrcodeContent || '',
                            qrcodeDataUrl: ''
                        })
                    }
                }
            }
        }
    }

    console.log('收集到的格口数据数量:', cellData.length)
    console.log('有二维码图片的数据数量:', cellData.filter(d => d.qrcodeDataUrl).length)

    return cellData
}

// 导出 Word 文档（使用 HTML 转 Word 的方式，更可靠）
export async function exportToWord(cellData: CellExportData[], onProgress?: (msg: string) => void): Promise<void> {
    if (cellData.length === 0) {
        throw new Error('没有可导出的格口数据')
    }

    onProgress?.('正在生成Word文档...')

    // 按柜子分组
    const groupedByCabinet = new Map<number, CellExportData[]>()
    for (const item of cellData) {
        if (!groupedByCabinet.has(item.cabinetId)) {
            groupedByCabinet.set(item.cabinetId, [])
        }
        groupedByCabinet.get(item.cabinetId)!.push(item)
    }

    // HTML 转义函数
    const escapeHtml = (str: string): string => {
        if (!str) return ''
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;')
    }

    // 生成 HTML 内容（Word 可以打开 HTML 文件）
    let htmlContent = `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>格口信息导出报告</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Microsoft YaHei', SimSun, '宋体', Arial, sans-serif;
            padding: 40px 20px;
            background: white;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        .header {
            text-align: center;
            margin-bottom: 30px;
        }
        .header h1 {
            font-size: 28px;
            color: #333;
            margin-bottom: 10px;
        }
        .header .time {
            font-size: 12px;
            color: #666;
        }
        .stats {
            text-align: center;
            margin-bottom: 25px;
            padding: 10px;
            background: #f0f0f0;
            border-radius: 8px;
            display: inline-block;
            width: auto;
            margin-left: auto;
            margin-right: auto;
        }
        .stats span {
            font-weight: bold;
            color: #2c7da0;
            font-size: 20px;
        }
        .cabinet-section {
            margin-bottom: 30px;
            border: 1px solid #ddd;
            border-radius: 8px;
            overflow: hidden;
            page-break-inside: avoid;
        }
        .cabinet-title {
            background: #2c7da0;
            color: white;
            padding: 12px 20px;
            font-size: 18px;
            font-weight: bold;
        }
        table {
            width: 100%;
            border-collapse: collapse;
        }
        th, td {
            padding: 10px 12px;
            text-align: center;
            border: 1px solid #ddd;
            vertical-align: middle;
        }
        th {
            background: #f5f5f5;
            font-weight: bold;
        }
        .qrcode-img {
            width: 80px;
            height: 80px;
            display: block;
            margin: 0 auto;
        }
        .qrcode-content {
            font-size: 10px;
            word-break: break-all;
            font-family: monospace;
            max-width: 200px;
        }
        .no-qrcode {
            color: #999;
        }
        .footer {
            text-align: center;
            margin-top: 30px;
            padding: 15px;
            color: #666;
            font-size: 11px;
            border-top: 1px solid #eee;
        }
        @media print {
            body {
                padding: 20px;
            }
            .cabinet-section {
                break-inside: avoid;
                page-break-inside: avoid;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📦 格口信息导出报告</h1>
            <div class="time">导出时间：${new Date().toLocaleString()}</div>
        </div>
        <div style="text-align: center;">
            <div class="stats">
                📊 总计格口数量：<span>${cellData.length}</span> 个
            </div>
        </div>
`

    // 遍历每个柜子
    for (const [cabinetId, items] of groupedByCabinet) {
        const cabinetName = items[0].cabinetName

        htmlContent += `
        <div class="cabinet-section">
            <div class="cabinet-title">
                🗄️ ${escapeHtml(cabinetName)}
            </div>
            <table>
                <thead>
                    <tr>
                        <th style="width:15%">柜子名称</th>
                        <th style="width:10%">格口号</th>
                        <th style="width:20%">工具名称</th>
                        <th style="width:20%">二维码</th>
                        <th style="width:35%">二维码内容</th>
                    </tr>
                </thead>
                <tbody>
`

        for (const item of items) {
            htmlContent += `
                    <tr>
                        <td>${escapeHtml(item.cabinetName)}</td>
                        <td>${item.slotNumber}</td>
                        <td>${escapeHtml(item.toolName)}</td>
                        <td>
                            ${item.qrcodeDataUrl
                ? `<img src="${item.qrcodeDataUrl}" class="qrcode-img" alt="二维码" />`
                : '<span class="no-qrcode">❌ 无二维码</span>'}
                        </td>
                        <td class="qrcode-content">${escapeHtml(item.qrcodeContent)}</td>
                    </tr>
`
        }

        htmlContent += `
                </tbody>
            </table>
        </div>
`
    }

    htmlContent += `
        <div class="footer">
            <p>生成时间：${new Date().toLocaleString()} | 格口管理系统导出</p>
            <p>※ 此文件可在 Microsoft Word 中打开，二维码图片为内嵌图片，可直接打印</p>
        </div>
    </div>
</body>
</html>`

    // 将 HTML 保存为 .doc 文件（Word 可以直接打开）
    const blob = new Blob([htmlContent], { type: 'application/msword' })
    saveAs(blob, `格口信息导出_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.doc`)
}

// 导出 Excel 文件
export async function exportToExcel(cellData: CellExportData[], onProgress?: (msg: string) => void): Promise<void> {
    if (cellData.length === 0) {
        throw new Error('没有可导出的格口数据')
    }

    onProgress?.('正在生成Excel文件...')

    // 准备数据
    const dataForSheet: any[][] = []

    // 添加表头
    dataForSheet.push(['柜子名称', '格口号', '工具名称', '二维码内容'])

    // 添加数据
    for (const item of cellData) {
        dataForSheet.push([
            item.cabinetName,
            item.slotNumber,
            item.toolName,
            item.qrcodeContent
        ])
    }

    // 创建工作簿和工作表
    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.aoa_to_sheet(dataForSheet)

    // 设置列宽
    ws['!cols'] = [
        { wch: 15 },  // 柜子名称
        { wch: 12 },  // 格口号
        { wch: 25 },  // 工具名称
        { wch: 50 }   // 二维码内容
    ]

    // 添加工作表
    XLSX.utils.book_append_sheet(wb, ws, '格口信息')

    // 生成文件
    const excelBuffer = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([excelBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    saveAs(blob, `格口信息导出_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.xlsx`)
}

// 导出 HTML 文件（带真实二维码图片，可直接在浏览器中查看）
export async function exportToHtml(cellData: CellExportData[], onProgress?: (msg: string) => void): Promise<void> {
    if (cellData.length === 0) {
        throw new Error('没有可导出的格口数据')
    }

    onProgress?.('正在生成HTML文件...')

    // 按柜子分组
    const groupedByCabinet = new Map<number, CellExportData[]>()
    for (const item of cellData) {
        if (!groupedByCabinet.has(item.cabinetId)) {
            groupedByCabinet.set(item.cabinetId, [])
        }
        groupedByCabinet.get(item.cabinetId)!.push(item)
    }

    // HTML 转义函数
    const escapeHtml = (str: string): string => {
        if (!str) return ''
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;')
    }

    // 生成 HTML 内容
    let htmlContent = `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>格口信息导出报告</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 40px 20px;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
        }
        .header {
            text-align: center;
            margin-bottom: 40px;
            color: white;
        }
        .header h1 {
            font-size: 36px;
            margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
        }
        .header .time {
            font-size: 14px;
            opacity: 0.9;
        }
        .stats {
            text-align: center;
            margin-bottom: 30px;
            padding: 15px;
            background: rgba(255,255,255,0.95);
            border-radius: 12px;
            display: inline-block;
            width: auto;
            margin-left: auto;
            margin-right: auto;
            box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }
        .stats span {
            font-weight: bold;
            color: #667eea;
            font-size: 24px;
        }
        .cabinet-section {
            background: white;
            border-radius: 20px;
            margin-bottom: 30px;
            overflow: hidden;
            box-shadow: 0 10px 30px rgba(0,0,0,0.1);
        }
        .cabinet-title {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px 25px;
            font-size: 20px;
            font-weight: bold;
        }
        table {
            width: 100%;
            border-collapse: collapse;
        }
        th, td {
            padding: 12px 15px;
            text-align: center;
            border-bottom: 1px solid #e0e0e0;
            vertical-align: middle;
        }
        th {
            background: #f8f9fa;
            font-weight: bold;
            color: #333;
            border-bottom: 2px solid #667eea;
        }
        tr:hover {
            background: #f5f5f5;
        }
        .qrcode-img {
            width: 80px;
            height: 80px;
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 4px;
        }
        .qrcode-content {
            font-size: 11px;
            word-break: break-all;
            max-width: 250px;
            font-family: monospace;
        }
        .no-qrcode {
            color: #999;
            font-size: 12px;
        }
        .footer {
            text-align: center;
            margin-top: 40px;
            padding: 20px;
            color: white;
            font-size: 12px;
        }
        @media print {
            body {
                background: white;
                padding: 20px;
            }
            .cabinet-section {
                break-inside: avoid;
                page-break-inside: avoid;
                box-shadow: none;
                border: 1px solid #ddd;
            }
            .stats {
                background: white;
                border: 1px solid #ddd;
            }
        }
        @media (max-width: 768px) {
            th, td {
                padding: 8px 10px;
                font-size: 12px;
            }
            .qrcode-img {
                width: 60px;
                height: 60px;
            }
            .cabinet-title {
                font-size: 16px;
                padding: 10px 15px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📦 格口信息导出报告</h1>
            <div class="time">导出时间：${new Date().toLocaleString()}</div>
        </div>
        <div style="text-align: center;">
            <div class="stats">
                📊 总计格口数量：<span>${cellData.length}</span> 个
            </div>
        </div>
`

    // 遍历每个柜子
    for (const [cabinetId, items] of groupedByCabinet) {
        const cabinetName = items[0].cabinetName

        htmlContent += `
        <div class="cabinet-section">
            <div class="cabinet-title">
                🗄️ ${escapeHtml(cabinetName)}
            </div>
            <table>
                <thead>
                    <tr>
                        <th>柜子名称</th>
                        <th>格口号</th>
                        <th>工具名称</th>
                        <th>二维码</th>
                        <th>二维码内容</th>
                    </tr>
                </thead>
                <tbody>
`

        for (const item of items) {
            htmlContent += `
                    <tr>
                        <td>${escapeHtml(item.cabinetName)}</td>
                        <td>${item.slotNumber}</td>
                        <td>${escapeHtml(item.toolName)}</td>
                        <td>
                            ${item.qrcodeDataUrl
                ? `<img src="${item.qrcodeDataUrl}" class="qrcode-img" alt="二维码" />`
                : '<span class="no-qrcode">❌ 无二维码</span>'}
                        </td>
                        <td class="qrcode-content">${escapeHtml(item.qrcodeContent)}</td>
                    </tr>
`
        }

        htmlContent += `
                </tbody>
            </table>
        </div>
`
    }

    htmlContent += `
        <div class="footer">
            <p>生成时间：${new Date().toLocaleString()} | 格口管理系统导出</p>
            <p style="margin-top: 10px;">* 此文件包含真实二维码图片，可直接打印或分享</p>
        </div>
    </div>
</body>
</html>`

    // 下载 HTML 文件
    const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' })
    saveAs(blob, `格口信息导出_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.html`)
}
