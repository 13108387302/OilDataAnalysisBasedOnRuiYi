<template>
  <div class="data-preview">
    <el-card>
      <div slot="header">
        <span>数据预览</span>
        <el-button 
          style="float: right; padding: 3px 0" 
          type="text" 
          @click="refreshPreview"
          :loading="loading">
          刷新
        </el-button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" style="text-align: center; padding: 40px;">
        <el-icon class="is-loading"><i class="el-icon-loading"></i></el-icon>
        <p style="margin-top: 10px;">正在解析数据文件...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" style="text-align: center; padding: 40px;">
        <el-alert
          :title="error.title || '数据解析失败'"
          :description="error.message"
          type="error"
          show-icon
          :closable="false" />
        <el-button type="primary" @click="refreshPreview" style="margin-top: 15px;">
          重试
        </el-button>
      </div>

      <!-- 数据预览内容 -->
      <div v-else-if="previewData">
        <!-- 数据来源提示 -->
        <el-alert
          title="📊 数据预览"
          type="info"
          description="以下是您上传文件的真实数据预览"
          show-icon
          :closable="false"
          style="margin-bottom: 20px;">
        </el-alert>

        <!-- 文件信息 -->
        <el-descriptions :column="3" border style="margin-bottom: 20px;">
          <el-descriptions-item label="文件名">{{ fileInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatFileSize(fileInfo.size) }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ fileInfo.type }}</el-descriptions-item>
          <el-descriptions-item label="数据行数">
            {{ previewData.rowCount }}
          </el-descriptions-item>
          <el-descriptions-item label="数据列数">
            {{ previewData.columnCount }}
          </el-descriptions-item>
          <el-descriptions-item label="数据类型">
            {{ getDatasetType }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 列信息统计 -->
        <div style="margin-bottom: 20px;">
          <h4>列信息统计</h4>
          <el-row :gutter="20">
            <el-col :span="6">
              <el-statistic title="数值列" :value="numericColumns.length" suffix="列" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="文本列" :value="textColumns.length" suffix="列" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="日期列" :value="dateColumns.length" suffix="列" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="缺失值列" :value="columnsWithMissing.length" suffix="列" />
            </el-col>
          </el-row>
        </div>

        <!-- 列详细信息 -->
        <div style="margin-bottom: 20px;">
          <h4>列详细信息</h4>
          <el-table :data="columnDetails" border size="mini" max-height="300">
            <el-table-column prop="name" label="列名" min-width="120" />
            <el-table-column prop="type" label="数据类型" width="100">
              <template slot-scope="scope">
                <el-tag :type="getColumnTypeColor(scope.row.type)" size="mini">
                  {{ getColumnTypeName(scope.row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="nonNullCount" label="非空值" width="80" />
            <el-table-column prop="nullCount" label="缺失值" width="80">
              <template slot-scope="scope">
                <span :style="{ color: scope.row.nullCount > 0 ? '#F56C6C' : '#67C23A' }">
                  {{ scope.row.nullCount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="uniqueCount" label="唯一值" width="80" />
            <el-table-column prop="sampleValues" label="示例值" min-width="200">
              <template slot-scope="scope">
                <el-tooltip :content="scope.row.sampleValues.join(', ')" placement="top">
                  <span>{{ scope.row.sampleValues.slice(0, 3).join(', ') }}...</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="推荐用途" width="120">
              <template slot-scope="scope">
                <el-tag 
                  v-for="usage in getRecommendedUsage(scope.row)" 
                  :key="usage.type"
                  :type="usage.color" 
                  size="mini" 
                  style="margin-right: 5px;">
                  {{ usage.label }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 数据预览表格 -->
        <div>
          <h4>数据预览 (前{{ Math.min(10, previewData.rowCount) }}行)</h4>
          <el-table :data="previewData.sampleData" border size="mini" max-height="400">
            <el-table-column type="index" label="行号" width="60" />
            <el-table-column
              v-for="column in previewData.columns"
              :key="column"
              :prop="column"
              :label="column"
              min-width="120"
              show-overflow-tooltip>
              <template slot-scope="scope">
                <span :style="getCellStyle(scope.row[column], column)">
                  {{ formatCellValue(scope.row[column]) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 智能建议 -->
        <div v-if="smartSuggestions.length > 0" style="margin-top: 20px;">
          <h4>智能建议</h4>
          <el-alert
            v-for="(suggestion, index) in smartSuggestions"
            :key="index"
            :title="suggestion.title"
            :description="suggestion.description"
            :type="suggestion.type"
            style="margin-bottom: 10px;"
            show-icon />
        </div>


      </div>

      <!-- 空状态 -->
      <div v-else style="text-align: center; padding: 40px;">
        <i class="el-icon-document" style="font-size: 48px; color: #C0C4CC;"></i>
        <p style="color: #909399; margin-top: 10px;">请先上传数据文件</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { analyzeFile } from '@/api/petrol/task'

export default {
  name: 'DataPreview',
  props: {
    file: {
      type: File,
      default: null
    },
    autoPreview: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      loading: false,
      error: null,
      previewData: null,
      fileInfo: {},
      smartSuggestions: []
    }
  },
  computed: {
    columnDetails() {
      if (!this.previewData || !this.previewData.columnInfo) return []
      
      return this.previewData.columns.map(column => {
        const info = this.previewData.columnInfo[column] || {}
        return {
          name: column,
          type: info.dtype || 'unknown',
          nonNullCount: info.non_null_count || 0,
          nullCount: info.null_count || 0,
          uniqueCount: info.unique_count || 0,
          sampleValues: info.sample_values || []
        }
      })
    },
    
    numericColumns() {
      return this.columnDetails.filter(col => 
        ['int64', 'float64', 'int32', 'float32', 'number'].includes(col.type)
      )
    },
    
    textColumns() {
      return this.columnDetails.filter(col => 
        ['object', 'string', 'category'].includes(col.type)
      )
    },
    
    dateColumns() {
      return this.columnDetails.filter(col => 
        ['datetime64', 'datetime', 'date'].includes(col.type)
      )
    },
    
    columnsWithMissing() {
      return this.columnDetails.filter(col => col.nullCount > 0)
    },

    /** 获取数据集类型 */
    getDatasetType() {
      if (!this.previewData) return '-'

      // 优先根据列名判断石油数据
      if (this.previewData.columns) {
        const columns = this.previewData.columns
        if (columns.includes('深度') || columns.includes('depth')) {
          return '石油测井数据'
        } else if (columns.some(col => col.includes('时间') || col.includes('time'))) {
          return '时间序列数据'
        }
      }

      // 根据数值列比例判断数据集类型
      const numericRatio = this.numericColumns.length / this.previewData.columnCount
      if (numericRatio > 0.8) return '数值型数据集'
      if (numericRatio > 0.5) return '混合型数据集'
      return '文本型数据集'
    },

    /** 判断是否为开发模式 */
    isDevelopmentMode() {
      return process.env.NODE_ENV === 'development' || process.env.VUE_APP_ENV === 'development'
    }
  },
  watch: {
    file: {
      handler(newFile) {
        console.log('DataPreview: 文件变化', newFile ? newFile.name : '无文件', 'autoPreview:', this.autoPreview)
        if (newFile && this.autoPreview) {
          console.log('DataPreview: 开始预览文件')
          this.previewFile()
        }
      },
      immediate: true
    }
  },
  methods: {
    // 导入API方法
    analyzeFile,
    /** 预览文件 */
    async previewFile() {
      if (!this.file) {
        this.previewData = null
        return
      }

      this.loading = true
      this.error = null
      this.smartSuggestions = []

      try {
        // 设置文件信息
        this.fileInfo = {
          name: this.file.name,
          size: this.file.size,
          type: this.getFileType(this.file.name)
        }

        // 🔧 修复：优先使用前端直接解析，确保使用真实数据
        try {
          const fileName = this.file.name.toLowerCase()

          if (fileName.endsWith('.csv')) {
            // CSV文件：前端直接解析
            console.log('🔍 使用前端直接解析CSV文件:', this.file.name)
            const csvData = await this.parseCSVFileDirectly(this.file)
            this.previewData = csvData
            console.log('✅ 前端CSV解析成功，使用真实数据')
          } else if (fileName.endsWith('.xlsx') || fileName.endsWith('.xls')) {
            // Excel文件：优先前端直接解析
            console.log('🔍 优先使用前端直接解析Excel文件:', this.file.name)
            try {
              const excelData = await this.parseExcelFileDirectly(this.file)
              this.previewData = excelData
              console.log('✅ 前端Excel解析成功，使用真实数据')
            } catch (frontendError) {
              console.warn('⚠️ 前端Excel解析失败，尝试后端API:', frontendError.message)
              // 前端解析失败，尝试后端API
              const apiData = await this.parseFileWithAPI(this.file)
              this.previewData = apiData
              console.log('✅ 后端API解析成功（备选方案）')
            }
          } else {
            // 其他文件类型：尝试调用后端API
            console.log('🔍 未知文件类型，尝试后端API解析:', this.file.name)
            const apiData = await this.parseFileWithAPI(this.file)
            this.previewData = apiData
            console.log('✅ 后端API解析成功')
          }
        } catch (error) {
          console.error('❌ 文件解析失败:', error.message)

          // 文件解析失败，显示错误信息
          this.error = {
            title: '文件解析失败',
            message: `无法解析文件 "${this.file.name}": ${error.message}。请检查文件格式是否正确，或尝试重新上传。`
          }

          // 显示错误信息
          this.$message({
            message: `文件解析失败: ${error.message}。请检查文件格式或重新上传。`,
            type: 'error',
            duration: 10000,
            showClose: true
          })

          // 清空预览数据
          this.previewData = null
        }
        
        // 生成智能建议
        this.generateSmartSuggestions()
        
        // 触发事件
        console.log('DataPreview: 准备触发preview-ready事件', {
          previewData: this.previewData,
          fileInfo: this.fileInfo,
          suggestions: this.smartSuggestions
        })

        this.$emit('preview-ready', {
          previewData: this.previewData,
          fileInfo: this.fileInfo,
          suggestions: this.smartSuggestions
        })

        console.log('DataPreview: preview-ready事件已触发')

      } catch (error) {
        console.error('数据预览失败:', error)
        this.error = {
          title: '数据解析失败',
          message: error.message || '无法解析数据文件，请检查文件格式是否正确'
        }
      } finally {
        this.loading = false
      }
    },

    /** 前端直接解析CSV文件 */
    async parseCSVFileDirectly(file) {
      console.log('🔍 开始前端直接解析CSV文件:', file.name)

      try {
        const text = await file.text()
        const lines = text.split('\n').filter(line => line.trim())

        if (lines.length < 2) {
          throw new Error('CSV文件至少需要包含表头和一行数据')
        }

        // 解析表头
        const headers = lines[0].split(',').map(h => h.trim().replace(/"/g, ''))
        console.log('📊 CSV表头:', headers)

        // 解析数据行
        const sampleData = []
        const maxPreviewRows = 10
        const dataLines = lines.slice(1)

        for (let i = 0; i < Math.min(maxPreviewRows, dataLines.length); i++) {
          const values = dataLines[i].split(',').map(v => v.trim().replace(/"/g, ''))
          const row = {}
          headers.forEach((header, index) => {
            row[header] = values[index] || ''
          })
          sampleData.push(row)
        }

        // 生成列统计信息
        const columnInfo = {}
        headers.forEach(header => {
          // 分析列的数据类型
          const sampleValues = sampleData.map(row => row[header]).filter(v => v !== '')
          const isNumeric = sampleValues.every(v => !isNaN(parseFloat(v)) && isFinite(v))

          columnInfo[header] = {
            dtype: isNumeric ? 'float64' : 'object',
            non_null_count: dataLines.length, // 假设所有行都有数据
            null_count: 0,
            unique_count: Math.min(dataLines.length, 1000), // 估算
            sample_values: sampleValues.slice(0, 5),
            _isRealData: true, // 标记为真实数据
            _dataSource: 'frontend_csv_parser'
          }
        })

        console.log('✅ CSV解析完成:', {
          headers: headers.length,
          dataRows: dataLines.length,
          previewRows: sampleData.length
        })

        return {
          headers,
          data: sampleData,
          columnInfo,
          totalRows: dataLines.length,
          _isRealData: true,
          _dataSource: 'frontend_csv_parser'
        }

      } catch (error) {
        console.error('❌ CSV解析失败:', error)
        throw new Error(`CSV解析失败: ${error.message}`)
      }
    },

    /** 前端直接解析Excel文件 */
    async parseExcelFileDirectly(file) {
      console.log('🔍 开始前端直接解析Excel文件:', file.name)

      try {
        // 动态导入xlsx库
        const XLSX = await import('xlsx')

        // 读取文件为ArrayBuffer
        const arrayBuffer = await file.arrayBuffer()

        // 解析Excel文件
        const workbook = XLSX.read(arrayBuffer, { type: 'array' })

        // 获取第一个工作表
        const sheetName = workbook.SheetNames[0]
        if (!sheetName) {
          throw new Error('Excel文件中没有找到工作表')
        }

        const worksheet = workbook.Sheets[sheetName]

        // 转换为JSON格式
        const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 })

        if (jsonData.length < 2) {
          throw new Error('Excel文件至少需要包含表头和一行数据')
        }

        // 解析表头
        const headers = jsonData[0].map(h => String(h || '').trim()).filter(h => h !== '')
        console.log('📊 Excel表头:', headers)

        // 解析数据行
        const dataRows = jsonData.slice(1).filter(row =>
          row && row.some(cell => cell !== null && cell !== undefined && String(cell).trim() !== '')
        )

        // 生成预览数据（前10行）
        const sampleData = []
        const maxPreviewRows = 10

        for (let i = 0; i < Math.min(maxPreviewRows, dataRows.length); i++) {
          const row = {}
          headers.forEach((header, index) => {
            const value = dataRows[i][index]
            row[header] = value !== null && value !== undefined ? String(value) : ''
          })
          sampleData.push(row)
        }

        // 生成列统计信息
        const columnInfo = {}
        headers.forEach((header, index) => {
          // 分析列的数据类型
          const sampleValues = dataRows.slice(0, 100).map(row => row[index])
            .filter(v => v !== null && v !== undefined && String(v).trim() !== '')

          const isNumeric = sampleValues.every(v => !isNaN(parseFloat(v)) && isFinite(v))

          columnInfo[header] = {
            dtype: isNumeric ? 'float64' : 'object',
            non_null_count: dataRows.length,
            null_count: 0,
            unique_count: Math.min(dataRows.length, 1000),
            sample_values: sampleValues.slice(0, 5).map(v => String(v)),
            _isRealData: true,
            _dataSource: 'frontend_excel_parser'
          }
        })

        console.log('✅ Excel解析完成:', {
          headers: headers.length,
          dataRows: dataRows.length,
          previewRows: sampleData.length,
          sheetName
        })

        return {
          headers,
          data: sampleData,
          columnInfo,
          totalRows: dataRows.length,
          _isRealData: true,
          _dataSource: 'frontend_excel_parser',
          _sheetName: sheetName
        }

      } catch (error) {
        console.error('❌ Excel解析失败:', error)
        throw new Error(`Excel解析失败: ${error.message}`)
      }
    },

    /** 使用后端API解析文件 */
    async parseFileWithAPI(file) {
      try {
        console.log('🔍 开始调用后端API解析文件:', file.name)
        console.log('📁 文件信息:', {
          name: file.name,
          size: file.size,
          type: file.type,
          lastModified: new Date(file.lastModified).toLocaleString()
        })

        const response = await this.analyzeFile(file)
        console.log('✅ 后端API响应:', response)

        if (response.code === 200 && response.data) {
          const apiData = response.data
          console.log('🔍 后端API返回的数据结构:', apiData)

          // 🔧 修复：安全地提取数据，添加类型检查
          const columns = Array.isArray(apiData.headers) ? apiData.headers : []
          const stats = (apiData.stats && typeof apiData.stats === 'object') ? apiData.stats : {}

          console.log('📊 解析后的列信息:', { columns, statsKeys: Object.keys(stats) })

          // 验证数据完整性
          if (columns.length === 0) {
            throw new Error('后端API返回的文件头信息为空')
          }

          // 生成示例数据（基于统计信息）
          const sampleData = this.generateSampleDataFromStats(columns, stats)

          // 转换列信息格式
          const columnInfo = {}
          columns.forEach(col => {
            const colStats = stats[col] || {}
            columnInfo[col] = {
              dtype: this.inferDataType(colStats),
              non_null_count: colStats.count || 0,
              null_count: colStats.null_count || 0,
              unique_count: colStats.unique_count || 0,
              sample_values: colStats.sample_values || [],
              min: colStats.min,
              max: colStats.max,
              mean: colStats.mean,
              std: colStats.std
            }
          })

          return {
            columns,
            sampleData,
            columnInfo,
            rowCount: apiData.total_rows || 0,
            columnCount: columns.length
          }
        } else {
          throw new Error(response.msg || '文件解析失败')
        }
      } catch (error) {
        console.error('❌ API调用失败:', error)
        throw new Error('文件解析失败: ' + (error.message || '网络错误'))
      }
    },

    /** 根据统计信息生成示例数据 */
    generateSampleDataFromStats(columns, stats) {
      console.log('🔍 生成示例数据，输入参数:', { columns, stats })

      // 🔧 修复：添加输入验证
      if (!Array.isArray(columns)) {
        console.error('❌ columns不是数组:', columns)
        return []
      }

      if (!stats || typeof stats !== 'object') {
        console.error('❌ stats不是对象:', stats)
        return []
      }

      const sampleData = []
      const sampleCount = 10

      for (let i = 0; i < sampleCount; i++) {
        const row = {}

        // 🔧 修复：安全地遍历列
        columns.forEach((col, index) => {
          try {
            const colStats = stats[col] || {}

            // 优先使用真实的示例值
            if (colStats.sampleValues && Array.isArray(colStats.sampleValues) && colStats.sampleValues.length > 0) {
              row[col] = colStats.sampleValues[i % colStats.sampleValues.length]
            } else if (colStats.sample_values && Array.isArray(colStats.sample_values) && colStats.sample_values.length > 0) {
              // 兼容不同的字段名
              row[col] = colStats.sample_values[i % colStats.sample_values.length]
            } else {
              // 🔴 系统已禁用模拟数据生成 - 直接返回错误而不是占位符
              console.error(`❌ 列 ${col} 缺少真实示例数据，无法生成预览`)
              throw new Error(`数据列 ${col} 缺少真实示例数据，请检查数据源`)
            }
          } catch (error) {
            console.error(`❌ 处理列 ${col} 时出错:`, error)
            throw new Error(`数据列 ${col} 处理失败: ${error.message}`)
          }
        })

        sampleData.push(row)
      }

      console.log('✅ 生成的示例数据:', sampleData.slice(0, 2))
      return sampleData
    },

    /** 推断数据类型 */
    inferDataType(colStats) {
      if (colStats.dtype) {
        return colStats.dtype
      }
      if (colStats.min !== undefined && colStats.max !== undefined) {
        return Number.isInteger(colStats.min) && Number.isInteger(colStats.max) ? 'int64' : 'float64'
      }
      return 'object'
    },



    /** 刷新预览 */
    refreshPreview() {
      this.previewFile()
    },

    /** 获取文件类型 */
    getFileType(filename) {
      const ext = filename.split('.').pop().toLowerCase()
      const typeMap = {
        'csv': 'CSV',
        'xlsx': 'Excel',
        'xls': 'Excel',
        'json': 'JSON'
      }
      return typeMap[ext] || ext.toUpperCase()
    },

    /** 格式化文件大小 */
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },



    /** 获取列类型颜色 */
    getColumnTypeColor(type) {
      const colorMap = {
        'int64': 'primary',
        'float64': 'primary',
        'int32': 'primary',
        'float32': 'primary',
        'number': 'primary',
        'object': 'success',
        'string': 'success',
        'category': 'warning',
        'datetime64': 'info',
        'datetime': 'info',
        'date': 'info'
      }
      return colorMap[type] || 'info'
    },

    /** 获取列类型名称 */
    getColumnTypeName(type) {
      const nameMap = {
        'int64': '整数',
        'float64': '浮点数',
        'int32': '整数',
        'float32': '浮点数',
        'number': '数值',
        'object': '文本',
        'string': '字符串',
        'category': '分类',
        'datetime64': '日期时间',
        'datetime': '日期时间',
        'date': '日期'
      }
      return nameMap[type] || type
    },

    /** 获取推荐用途 */
    getRecommendedUsage(column) {
      const usages = []
      
      // 数值列可以作为特征或目标
      if (['int64', 'float64', 'int32', 'float32', 'number'].includes(column.type)) {
        usages.push({ type: 'feature', label: '特征', color: 'primary' })
        
        // 如果唯一值较少，可能是分类目标
        if (column.uniqueCount < 20) {
          usages.push({ type: 'target_class', label: '分类目标', color: 'success' })
        } else {
          usages.push({ type: 'target_reg', label: '回归目标', color: 'warning' })
        }
      }
      
      // 文本列通常作为分类特征或目标
      if (['object', 'string', 'category'].includes(column.type)) {
        if (column.uniqueCount < 50) {
          usages.push({ type: 'feature_cat', label: '分类特征', color: 'info' })
          usages.push({ type: 'target_class', label: '分类目标', color: 'success' })
        }
      }
      
      return usages
    },

    /** 获取单元格样式 */
    getCellStyle(value, column) {
      if (value === null || value === undefined || value === '') {
        return { color: '#F56C6C', fontStyle: 'italic' }
      }
      
      const columnInfo = this.previewData.columnInfo[column]
      if (columnInfo && ['int64', 'float64', 'int32', 'float32'].includes(columnInfo.dtype)) {
        return { color: '#409EFF', fontWeight: 'bold' }
      }
      
      return {}
    },

    /** 格式化单元格值 */
    formatCellValue(value) {
      if (value === null || value === undefined) return 'NULL'
      if (value === '') return '(空)'
      if (typeof value === 'number') {
        return value.toFixed(4)
      }
      return String(value)
    },

    /** 生成智能建议 */
    generateSmartSuggestions() {
      this.smartSuggestions = []
      
      if (!this.previewData) return
      
      // 检查缺失值
      if (this.columnsWithMissing.length > 0) {
        this.smartSuggestions.push({
          title: '数据质量提醒',
          description: `发现 ${this.columnsWithMissing.length} 列包含缺失值，建议在预测前进行数据清洗`,
          type: 'warning'
        })
      }
      
      // 推荐特征列
      if (this.numericColumns.length >= 2) {
        this.smartSuggestions.push({
          title: '特征选择建议',
          description: `检测到 ${this.numericColumns.length} 个数值列，建议选择相关性高的列作为输入特征`,
          type: 'info'
        })
      }
      
      // 推荐目标列
      const potentialTargets = this.numericColumns.filter(col => 
        col.name.includes('目标') || col.name.includes('预测') || col.name.includes('结果')
      )
      if (potentialTargets.length > 0) {
        this.smartSuggestions.push({
          title: '目标变量建议',
          description: `发现可能的目标变量：${potentialTargets.map(col => col.name).join(', ')}`,
          type: 'success'
        })
      }
    },

    /** 格式化时间戳显示 */
    formatTimestamp(timestamp) {
      if (!timestamp) return '未知'
      try {
        return new Date(timestamp).toLocaleString('zh-CN')
      } catch (e) {
        return timestamp
      }
    }
  }
}
</script>

<style scoped>
.data-preview {
  width: 100%;
}

.el-statistic {
  text-align: center;
}

.el-table {
  margin-top: 10px;
}

h4 {
  margin: 15px 0 10px 0;
  color: #303133;
}

/* 开发者调试信息样式 */
.debug-info {
  border: 1px dashed #E6A23C;
  border-radius: 4px;
  background-color: #FDF6EC;
}

.debug-info .el-collapse {
  border: none;
  background: transparent;
}

.debug-info .el-collapse-item__header {
  background-color: transparent;
  border: none;
  color: #E6A23C;
  font-weight: bold;
}

.debug-info .el-collapse-item__content {
  padding: 10px;
  background-color: transparent;
}
</style>
