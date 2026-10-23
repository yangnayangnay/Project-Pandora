import { spawn, execSync } from 'child_process'
import { createServer } from 'net'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const BACKEND_DIR = resolve(__dirname, '..', '..', 'pandora-backend')
const JAR_PATH = resolve(BACKEND_DIR, 'target', 'pandora-backend-1.0.0.jar')
const BACKEND_PORT = 8080

function isPortListening(port) {
  return new Promise((resolvePromise) => {
    const tester = createServer()
    tester.once('error', () => resolvePromise(true))
    tester.once('listening', () => {
      tester.close(() => resolvePromise(false))
    })
    tester.listen(port)
  })
}

function waitForPort(port, timeoutMs = 30000) {
  return new Promise((resolvePromise, reject) => {
    const start = Date.now()
    const check = async () => {
      if (await isPortListening(port)) {
        resolvePromise()
      } else if (Date.now() - start > timeoutMs) {
        reject(new Error(`后端启动超时（${timeoutMs / 1000}s），请检查 ${JAR_PATH} 是否存在`))
      } else {
        setTimeout(check, 1000)
      }
    }
    check()
  })
}

async function main() {
  // R43 REQ-C: 云端模式（默认）不依赖本地后端——直接起 Vite（/api 代理指云）；
  // --local 参数或 VITE_API_TARGET=local 环境变量切回本地模式（自启本地 JAR）
  const useLocal = process.env.VITE_API_TARGET === 'local' || process.argv.includes('--local')
  process.env.VITE_API_TARGET = useLocal ? 'local' : ''
  if (!useLocal) {
    console.log('[启动脚本] 云端数据链路模式（/api -> http://123.56.153.138:8080），跳过本地后端')
    const vite = spawn('npx', ['vite'], {
      cwd: resolve(__dirname, '..'),
      stdio: 'inherit',
      shell: true
    })
    vite.on('exit', (code) => process.exit(code ?? 0))
    return
  }

  const backendRunning = await isPortListening(BACKEND_PORT)

  if (!backendRunning) {
    console.log('[启动脚本] 后端未运行，正在自动启动...')
    const backend = spawn('java', ['-jar', JAR_PATH], {
      cwd: BACKEND_DIR,
      stdio: ['ignore', 'pipe', 'pipe'],
      shell: true
    })
    backend.stdout.on('data', (d) => process.stdout.write(`[backend] ${d}`))
    backend.stderr.on('data', (d) => process.stderr.write(`[backend] ${d}`))
    backend.on('exit', (code) => {
      console.log(`[启动脚本] 后端进程退出，code=${code}`)
      process.exit(code ?? 0)
    })
    process.on('SIGINT', () => {
      backend.kill('SIGINT')
      process.exit(0)
    })
    process.on('SIGTERM', () => {
      backend.kill('SIGTERM')
      process.exit(0)
    })
    await waitForPort(BACKEND_PORT)
    console.log('[启动脚本] 后端已就绪 (localhost:8080)')
  } else {
    console.log('[启动脚本] 后端已在运行 (localhost:8080)')
  }

  console.log('[启动脚本] 启动 Vite 开发服务器...')
  const vite = spawn('npx', ['vite'], {
    cwd: resolve(__dirname, '..'),
    stdio: 'inherit',
    shell: true
  })
  vite.on('exit', (code) => process.exit(code ?? 0))
}

main().catch((err) => {
  console.error('[启动脚本] 错误:', err.message)
  process.exit(1)
})