<template>
  <div class="virtual-list" ref="container" @scroll="handleScroll">
    <div class="virtual-list-phantom" :style="{ height: totalHeight + 'px' }"></div>
    <div class="virtual-list-content" :style="{ transform: `translateY(${startOffset}px)` }">
      <div
        v-for="item in visibleData"
        :key="item.index"
        class="virtual-list-item"
        :style="{ height: itemHeight + 'px' }">
        <slot :item="item.data" :index="item.index"></slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VirtualList',
  props: {
    // 数据列表
    data: {
      type: Array,
      required: true
    },
    // 每项高度
    itemHeight: {
      type: Number,
      default: 50
    },
    // 容器高度
    height: {
      type: Number,
      required: true
    },
    // 缓冲区大小（额外渲染的项目数）
    buffer: {
      type: Number,
      default: 5
    }
  },
  data() {
    return {
      scrollTop: 0,
      containerHeight: 0
    };
  },
  computed: {
    // 总高度
    totalHeight() {
      return this.data.length * this.itemHeight;
    },
    // 可视区域可容纳的项目数
    visibleCount() {
      return Math.ceil(this.containerHeight / this.itemHeight);
    },
    // 开始索引
    startIndex() {
      return Math.max(0, Math.floor(this.scrollTop / this.itemHeight) - this.buffer);
    },
    // 结束索引
    endIndex() {
      return Math.min(
        this.data.length - 1,
        this.startIndex + this.visibleCount + this.buffer * 2
      );
    },
    // 可视区域数据
    visibleData() {
      const result = [];
      for (let i = this.startIndex; i <= this.endIndex; i++) {
        if (this.data[i]) {
          result.push({
            index: i,
            data: this.data[i]
          });
        }
      }
      return result;
    },
    // 偏移量
    startOffset() {
      return this.startIndex * this.itemHeight;
    }
  },
  mounted() {
    this.containerHeight = this.$refs.container.clientHeight || this.height;
    // 监听容器大小变化
    this.resizeObserver = new ResizeObserver(entries => {
      for (let entry of entries) {
        this.containerHeight = entry.contentRect.height;
      }
    });
    this.resizeObserver.observe(this.$refs.container);
  },
  beforeDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
  },
  methods: {
    handleScroll(event) {
      this.scrollTop = event.target.scrollTop;
    },
    // 滚动到指定索引
    scrollToIndex(index) {
      const scrollTop = index * this.itemHeight;
      this.$refs.container.scrollTop = scrollTop;
    },
    // 滚动到顶部
    scrollToTop() {
      this.$refs.container.scrollTop = 0;
    },
    // 滚动到底部
    scrollToBottom() {
      this.$refs.container.scrollTop = this.totalHeight;
    }
  }
};
</script>

<style scoped>
.virtual-list {
  position: relative;
  overflow-y: auto;
  height: 100%;
}

.virtual-list-phantom {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  z-index: -1;
}

.virtual-list-content {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
}

.virtual-list-item {
  box-sizing: border-box;
}

/* 滚动条样式 */
.virtual-list::-webkit-scrollbar {
  width: 6px;
}

.virtual-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.virtual-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.virtual-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
