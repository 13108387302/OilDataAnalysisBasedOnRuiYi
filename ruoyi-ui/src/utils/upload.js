/**
 * 文件上传工具类 - 支持分片上传、进度监控、断点续传
 */
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 分片大小 (2MB)
const CHUNK_SIZE = 2 * 1024 * 1024

/**
 * 文件分片上传器
 */
export class ChunkUploader {
  constructor(options = {}) {
    this.file = options.file
    this.chunkSize = options.chunkSize || CHUNK_SIZE
    this.uploadUrl = options.uploadUrl || '/petrol/dataset/upload-chunk'
    this.mergeUrl = options.mergeUrl || '/petrol/dataset/merge-chunks'
    this.checkUrl = options.checkUrl || '/petrol/dataset/check-chunks'
    this.onProgress = options.onProgress || (() => {})
    this.onError = options.onError || (() => {})
    this.onSuccess = options.onSuccess || (() => {})
    
    this.chunks = []
    this.uploadedChunks = new Set()
    this.fileHash = ''
    this.isUploading = false
    this.isPaused = false
  }

  /**
   * 计算文件哈希值
   */
  async calculateFileHash() {
    return new Promise((resolve) => {
      const spark = new SparkMD5.ArrayBuffer()
      const fileReader = new FileReader()
      const chunks = Math.ceil(this.file.size / this.chunkSize)
      let currentChunk = 0

      fileReader.onload = (e) => {
        spark.append(e.target.result)
        currentChunk++

        if (currentChunk < chunks) {
          loadNext()
        } else {
          resolve(spark.end())
        }
      }

      const loadNext = () => {
        const start = currentChunk * this.chunkSize
        const end = Math.min(start + this.chunkSize, this.file.size)
        fileReader.readAsArrayBuffer(this.file.slice(start, end))
      }

      loadNext()
    })
  }

  /**
   * 创建文件分片
   */
  createChunks() {
    const chunks = []
    const totalChunks = Math.ceil(this.file.size / this.chunkSize)

    for (let i = 0; i < totalChunks; i++) {
      const start = i * this.chunkSize
      const end = Math.min(start + this.chunkSize, this.file.size)
      
      chunks.push({
        index: i,
        start,
        end,
        chunk: this.file.slice(start, end),
        hash: `${this.fileHash}-${i}`,
        size: end - start
      })
    }

    this.chunks = chunks
    return chunks
  }

  /**
   * 检查已上传的分片
   */
  async checkUploadedChunks() {
    try {
      const response = await request({
        url: this.checkUrl,
        method: 'post',
        data: {
          fileHash: this.fileHash,
          fileName: this.file.name,
          totalChunks: this.chunks.length
        }
      })

      if (response.data && response.data.uploadedChunks) {
        this.uploadedChunks = new Set(response.data.uploadedChunks)
      }

      return response.data
    } catch (error) {
      console.error('检查已上传分片失败:', error)
      return { uploadedChunks: [] }
    }
  }

  /**
   * 上传单个分片
   */
  async uploadChunk(chunkInfo) {
    const formData = new FormData()
    formData.append('chunk', chunkInfo.chunk)
    formData.append('chunkIndex', chunkInfo.index)
    formData.append('chunkHash', chunkInfo.hash)
    formData.append('fileHash', this.fileHash)
    formData.append('fileName', this.file.name)
    formData.append('totalChunks', this.chunks.length)

    try {
      const response = await request({
        url: this.uploadUrl,
        method: 'post',
        data: formData,
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': 'Bearer ' + getToken()
        },
        timeout: 30000 // 30秒超时
      })

      this.uploadedChunks.add(chunkInfo.index)
      return response
    } catch (error) {
      console.error(`分片 ${chunkInfo.index} 上传失败:`, error)
      throw error
    }
  }

  /**
   * 并发上传分片
   */
  async uploadChunks() {
    const pendingChunks = this.chunks.filter(chunk => 
      !this.uploadedChunks.has(chunk.index)
    )

    if (pendingChunks.length === 0) {
      return true
    }

    const concurrency = 3 // 并发数
    const uploadPromises = []

    for (let i = 0; i < pendingChunks.length; i += concurrency) {
      const batch = pendingChunks.slice(i, i + concurrency)
      
      const batchPromises = batch.map(async (chunk) => {
        if (this.isPaused) return
        
        try {
          await this.uploadChunk(chunk)
          
          // 更新进度
          const progress = (this.uploadedChunks.size / this.chunks.length) * 100
          this.onProgress({
            loaded: this.uploadedChunks.size * this.chunkSize,
            total: this.file.size,
            percentage: Math.round(progress),
            currentChunk: chunk.index,
            totalChunks: this.chunks.length
          })
        } catch (error) {
          this.onError(error, chunk)
          throw error
        }
      })

      uploadPromises.push(...batchPromises)
      
      // 等待当前批次完成再继续
      await Promise.all(batchPromises)
      
      if (this.isPaused) {
        break
      }
    }

    return true
  }

  /**
   * 合并分片
   */
  async mergeChunks(additionalData = {}) {
    try {
      const response = await request({
        url: this.mergeUrl,
        method: 'post',
        data: {
          fileHash: this.fileHash,
          fileName: this.file.name,
          totalChunks: this.chunks.length,
          fileSize: this.file.size,
          ...additionalData
        }
      })

      return response
    } catch (error) {
      console.error('合并分片失败:', error)
      throw error
    }
  }

  /**
   * 开始上传
   */
  async start(additionalData = {}) {
    if (this.isUploading) {
      console.warn('文件正在上传中...')
      return
    }

    try {
      this.isUploading = true
      this.isPaused = false

      // 1. 计算文件哈希
      this.onProgress({ stage: 'hashing', percentage: 0 })
      this.fileHash = await this.calculateFileHash()

      // 2. 创建分片
      this.onProgress({ stage: 'chunking', percentage: 10 })
      this.createChunks()

      // 3. 检查已上传的分片（断点续传）
      this.onProgress({ stage: 'checking', percentage: 20 })
      await this.checkUploadedChunks()

      // 4. 上传分片
      this.onProgress({ stage: 'uploading', percentage: 30 })
      await this.uploadChunks()

      // 5. 合并分片
      this.onProgress({ stage: 'merging', percentage: 90 })
      const result = await this.mergeChunks(additionalData)

      // 6. 完成
      this.onProgress({ stage: 'completed', percentage: 100 })
      this.onSuccess(result)

      return result
    } catch (error) {
      this.onError(error)
      throw error
    } finally {
      this.isUploading = false
    }
  }

  /**
   * 暂停上传
   */
  pause() {
    this.isPaused = true
  }

  /**
   * 恢复上传
   */
  async resume() {
    if (!this.isUploading) {
      return this.start()
    }
    
    this.isPaused = false
    await this.uploadChunks()
  }

  /**
   * 取消上传
   */
  cancel() {
    this.isPaused = true
    this.isUploading = false
    this.uploadedChunks.clear()
  }
}

/**
 * 简单文件上传（小文件）
 */
export function simpleUpload(file, options = {}) {
  const formData = new FormData()
  formData.append('file', file)
  
  Object.keys(options.data || {}).forEach(key => {
    formData.append(key, options.data[key])
  })

  return request({
    url: options.url || '/petrol/dataset/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': 'Bearer ' + getToken()
    },
    onUploadProgress: options.onProgress,
    timeout: options.timeout || 60000
  })
}

/**
 * 智能上传（根据文件大小选择上传方式）
 */
export function smartUpload(file, options = {}) {
  const threshold = options.chunkThreshold || 10 * 1024 * 1024 // 10MB

  if (file.size <= threshold) {
    // 小文件直接上传
    return simpleUpload(file, options)
  } else {
    // 大文件分片上传
    const uploader = new ChunkUploader({
      file,
      ...options
    })
    return uploader.start(options.data)
  }
}

export default {
  ChunkUploader,
  simpleUpload,
  smartUpload
}
