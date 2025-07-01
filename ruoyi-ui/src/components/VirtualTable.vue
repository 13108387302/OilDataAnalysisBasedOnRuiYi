<template>
  <div class="virtual-table-container" ref="container">
    <!-- 表头 -->
    <div class="virtual-table-header" ref="header">
      <table class="virtual-table">
        <thead>
          <tr>
            <th 
              v-for="(column, index) in columns" 
              :key="index"
              :style="{ width: column.width || 'auto', minWidth: column.minWidth || '100px' }"
              :class="column.className"
            >
              {{ column.label }}
            </th>
          </tr>
        </thead>
      </table>
    </div>

    <!-- 表体容器 -->
    <div 
      class="virtual-table-body" 
      ref="body"
      @scroll="handleScroll"
      :style="{ height: containerHeight + 'px' }"
    >
      <!-- 占位元素，用于撑开滚动条 -->
      <div 
        class="virtual-table-spacer" 
        :style="{ height: totalHeight + 'px' }"
      ></div>

      <!-- 可见行容器 -->
      <div 
        class="virtual-table-content"
        :style="{ transform: `translateY(${offsetY}px)` }"
      >
        <table class="virtual-table">
          <tbody>
            <tr 
              v-for="(item, index) in visibleData" 
              :key="getRowKey(item, startIndex + index)"
              :class="getRowClass(item, startIndex + index)"
              @click="handleRowClick(item, startIndex + index)"
            >
              <td 
                v-for="(column, colIndex) in columns" 
                :key="colIndex"
                :style="{ width: column.width || 'auto', minWidth: column.minWidth || '100px' }"
                :class="column.className"
              >
                <slot 
                  :name="column.prop" 
                  :row="item" 
                  :column="column" 
                  :index="startIndex + index"
                >
                  {{ getCellValue(item, column) }}
                </slot>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="virtual-table-loading">
      <i class="el-icon-loading"></i>
      <span>{{ loadingText }}</span>
    </div>

    <!-- 空数据状态 -->
    <div v-if="!loading && data.length === 0" class="virtual-table-empty">
      <slot name="empty">
        <el-empty :description="emptyText"></el-empty>
      </slot>
    </div>
  </div>
</template>

<script>
import { debounce } from '@/utils/performance'

export default {
  name: 'VirtualTable',
  props: {
    // 数据源
    data: {
      type: Array,
      default: () => []
    },
    // 列配置
    columns: {
      type: Array,
      default: () => []
    },
    // 行高
    rowHeight: {
      type: Number,
      default: 40
    },
    // 容器高度
    height: {
      type: [Number, String],
      default: 400
    },
    // 缓冲区大小（额外渲染的行数）
    bufferSize: {
      type: Number,
      default: 5
    },
    // 行唯一标识字段
    rowKey: {
      type: [String, Function],
      default: 'id'
    },
    // 行类名函数
    rowClassName: {
      type: Function,
      default: null
    },
    // 加载状态
    loading: {
      type: Boolean,
      default: false
    },
    // 加载文本
    loadingText: {
      type: String,
      default: '加载中...'
    },
    // 空数据文本
    emptyText: {
      type: String,
      default: '暂无数据'
    },
    // 是否启用懒加载
    lazy: {
      type: Boolean,
      default: false
    },
    // 懒加载阈值（距离底部多少像素时触发）
    lazyThreshold: {
      type: Number,
      default: 100
    }
  },
  data() {
    return {
      scrollTop: 0,
      containerHeight: 0,
      visibleData: [],
      startIndex: 0,
      endIndex: 0,
      offsetY: 0
    }
  },
  computed: {
    // 总高度
    totalHeight() {
      return this.data.length * this.rowHeight
    },
    // 可见行数
    visibleCount() {
      return Math.ceil(this.containerHeight / this.rowHeight)
    },
    // 实际渲染的行数（包含缓冲区）
    renderCount() {
      return this.visibleCount + this.bufferSize * 2
    }
  },
  watch: {
    data: {
      handler() {
        this.updateVisibleData()
      },
      immediate: true
    },
    height: {
      handler() {
        this.$nextTick(() => {
          this.updateContainerHeight()
          this.updateVisibleData()
        })
      },
      immediate: true
    }
  },
  mounted() {
    this.updateContainerHeight()
    this.updateVisibleData()
    
    // 监听窗口大小变化
    window.addEventListener('resize', this.handleResize)
    
    // 创建防抖的滚动处理函数
    this.debouncedScroll = debounce(this.updateVisibleData, 16) // 60fps
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    // 更新容器高度
    updateContainerHeight() {
      if (typeof this.height === 'number') {
        this.containerHeight = this.height
      } else {
        const container = this.$refs.container
        if (container) {
          this.containerHeight = container.clientHeight
        }
      }
    },

    // 处理滚动事件
    handleScroll(event) {
      this.scrollTop = event.target.scrollTop
      this.debouncedScroll()
      
      // 懒加载检测
      if (this.lazy) {
        this.checkLazyLoad(event.target)
      }
    },

    // 更新可见数据
    updateVisibleData() {
      if (this.data.length === 0) {
        this.visibleData = []
        return
      }

      // 计算开始和结束索引
      const startIndex = Math.floor(this.scrollTop / this.rowHeight)
      const endIndex = Math.min(
        startIndex + this.renderCount,
        this.data.length
      )

      // 应用缓冲区
      this.startIndex = Math.max(0, startIndex - this.bufferSize)
      this.endIndex = Math.min(this.data.length, endIndex + this.bufferSize)

      // 更新可见数据
      this.visibleData = this.data.slice(this.startIndex, this.endIndex)

      // 计算偏移量
      this.offsetY = this.startIndex * this.rowHeight
    },

    // 检查懒加载
    checkLazyLoad(target) {
      const { scrollTop, scrollHeight, clientHeight } = target
      const distanceToBottom = scrollHeight - scrollTop - clientHeight
      
      if (distanceToBottom <= this.lazyThreshold) {
        this.$emit('lazy-load')
      }
    },

    // 处理窗口大小变化
    handleResize() {
      this.updateContainerHeight()
      this.updateVisibleData()
    },

    // 获取行唯一标识
    getRowKey(row, index) {
      if (typeof this.rowKey === 'function') {
        return this.rowKey(row, index)
      }
      return row[this.rowKey] || index
    },

    // 获取行类名
    getRowClass(row, index) {
      if (typeof this.rowClassName === 'function') {
        return this.rowClassName(row, index)
      }
      return ''
    },

    // 获取单元格值
    getCellValue(row, column) {
      if (column.formatter && typeof column.formatter === 'function') {
        return column.formatter(row, column)
      }
      
      const keys = column.prop.split('.')
      let value = row
      for (const key of keys) {
        value = value?.[key]
      }
      return value
    },

    // 处理行点击
    handleRowClick(row, index) {
      this.$emit('row-click', row, index)
    },

    // 滚动到指定行
    scrollToRow(index) {
      if (index < 0 || index >= this.data.length) return
      
      const scrollTop = index * this.rowHeight
      this.$refs.body.scrollTop = scrollTop
    },

    // 滚动到顶部
    scrollToTop() {
      this.$refs.body.scrollTop = 0
    },

    // 滚动到底部
    scrollToBottom() {
      this.$refs.body.scrollTop = this.totalHeight
    },

    // 刷新表格
    refresh() {
      this.updateVisibleData()
    }
  }
}
</script>

<style scoped>
.virtual-table-container {
  position: relative;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.virtual-table-header {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
  overflow: hidden;
}

.virtual-table-body {
  position: relative;
  overflow: auto;
}

.virtual-table-spacer {
  position: absolute;
  top: 0;
  left: 0;
  width: 1px;
  pointer-events: none;
}

.virtual-table-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
}

.virtual-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.virtual-table th,
.virtual-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.virtual-table th {
  font-weight: 500;
  color: #909399;
  background-color: #fafafa;
}

.virtual-table tbody tr:hover {
  background-color: #f5f7fa;
}

.virtual-table-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.virtual-table-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
}
</style>
