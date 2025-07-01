<template>
  <div class="help-center">
    <el-container>
      <!-- 侧边栏导航 -->
      <el-aside width="280px" class="help-sidebar">
        <div class="help-search">
          <el-input
            v-model="searchQuery"
            placeholder="搜索帮助内容..."
            prefix-icon="el-icon-search"
            @input="handleSearch">
          </el-input>
        </div>
        
        <el-menu
          :default-active="activeSection"
          class="help-menu"
          @select="handleMenuSelect">
          <el-menu-item index="overview">
            <i class="el-icon-info"></i>
            <span>系统概览</span>
          </el-menu-item>
          
          <el-submenu index="dataset">
            <template slot="title">
              <i class="el-icon-folder"></i>
              <span>数据集管理</span>
            </template>
            <el-menu-item index="dataset-upload">数据上传</el-menu-item>
            <el-menu-item index="dataset-format">数据格式</el-menu-item>
            <el-menu-item index="dataset-preview">数据预览</el-menu-item>
          </el-submenu>
          
          <el-submenu index="analysis">
            <template slot="title">
              <i class="el-icon-data-analysis"></i>
              <span>分析任务</span>
            </template>
            <el-menu-item index="analysis-create">创建任务</el-menu-item>
            <el-menu-item index="analysis-params">参数配置</el-menu-item>
            <el-menu-item index="analysis-results">结果查看</el-menu-item>
          </el-submenu>
          
          <el-submenu index="model">
            <template slot="title">
              <i class="el-icon-cpu"></i>
              <span>模型管理</span>
            </template>
            <el-menu-item index="model-train">模型训练</el-menu-item>
            <el-menu-item index="model-upload">模型上传</el-menu-item>
            <el-menu-item index="model-evaluate">模型评估</el-menu-item>
          </el-submenu>
          
          <el-submenu index="prediction">
            <template slot="title">
              <i class="el-icon-magic-stick"></i>
              <span>预测功能</span>
            </template>
            <el-menu-item index="prediction-setup">预测配置</el-menu-item>
            <el-menu-item index="prediction-execute">执行预测</el-menu-item>
            <el-menu-item index="prediction-results">结果分析</el-menu-item>
          </el-submenu>
          
          <el-submenu index="visualization">
            <template slot="title">
              <i class="el-icon-pie-chart"></i>
              <span>数据可视化</span>
            </template>
            <el-menu-item index="viz-charts">图表类型</el-menu-item>
            <el-menu-item index="viz-config">配置选项</el-menu-item>
            <el-menu-item index="viz-export">导出功能</el-menu-item>
          </el-submenu>
          
          <el-menu-item index="faq">
            <i class="el-icon-question"></i>
            <span>常见问题</span>
          </el-menu-item>
          
          <el-menu-item index="contact">
            <i class="el-icon-message"></i>
            <span>联系支持</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <!-- 主内容区 -->
      <el-main class="help-content">
        <div class="help-header">
          <h1>{{ currentTitle }}</h1>
          <div class="help-actions">
            <el-button type="text" @click="printPage">
              <i class="el-icon-printer"></i> 打印
            </el-button>
            <el-button type="text" @click="sharePage">
              <i class="el-icon-share"></i> 分享
            </el-button>
          </div>
        </div>
        
        <div class="help-body" v-html="currentContent"></div>
        
        <!-- 反馈区域 -->
        <div class="help-feedback">
          <el-divider>这篇文档对您有帮助吗？</el-divider>
          <div class="feedback-buttons">
            <el-button @click="submitFeedback(true)" :class="{ active: feedback === true }">
              <i class="el-icon-thumb-up"></i> 有帮助
            </el-button>
            <el-button @click="submitFeedback(false)" :class="{ active: feedback === false }">
              <i class="el-icon-thumb-down"></i> 没帮助
            </el-button>
          </div>
          <el-input
            v-if="feedback === false"
            v-model="feedbackText"
            type="textarea"
            placeholder="请告诉我们如何改进这篇文档..."
            :rows="3"
            style="margin-top: 16px;">
          </el-input>
          <el-button
            v-if="feedback === false && feedbackText"
            type="primary"
            size="small"
            @click="submitDetailedFeedback"
            style="margin-top: 8px;">
            提交反馈
          </el-button>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'HelpCenter',
  data() {
    return {
      activeSection: 'overview',
      searchQuery: '',
      feedback: null,
      feedbackText: '',
      helpContent: {
        overview: {
          title: '石油预测系统概览',
          content: `
            <h2>欢迎使用石油预测系统</h2>
            <p>石油预测系统是一个专业的数据分析和机器学习平台，专门为石油行业设计。系统提供了完整的数据分析工作流程，从数据上传到模型训练，再到预测分析。</p>
            
            <h3>主要功能</h3>
            <ul>
              <li><strong>数据集管理</strong>：支持多种格式的数据上传、预览和管理</li>
              <li><strong>分析任务</strong>：提供多种机器学习算法进行数据分析</li>
              <li><strong>模型管理</strong>：支持模型训练、上传和版本管理</li>
              <li><strong>预测功能</strong>：使用训练好的模型进行预测分析</li>
              <li><strong>数据可视化</strong>：丰富的图表类型展示分析结果</li>
            </ul>
            
            <h3>快速开始</h3>
            <ol>
              <li>上传您的数据集到系统</li>
              <li>创建分析任务并配置参数</li>
              <li>查看分析结果和模型性能</li>
              <li>使用模型进行预测分析</li>
            </ol>
          `
        },
        'dataset-upload': {
          title: '数据上传指南',
          content: `
            <h2>数据上传指南</h2>
            <p>系统支持多种数据格式的上传，包括CSV、Excel等常见格式。</p>
            
            <h3>支持的文件格式</h3>
            <ul>
              <li>CSV文件（.csv）</li>
              <li>Excel文件（.xlsx, .xls）</li>
              <li>文件大小限制：100MB</li>
            </ul>
            
            <h3>数据要求</h3>
            <ul>
              <li>第一行应为列标题</li>
              <li>数据应为结构化格式</li>
              <li>避免空行和特殊字符</li>
              <li>数值列应使用数字格式</li>
            </ul>
            
            <h3>上传步骤</h3>
            <ol>
              <li>进入"数据集管理"页面</li>
              <li>点击"上传数据集"按钮</li>
              <li>选择文件并填写基本信息</li>
              <li>预览数据确认无误后提交</li>
            </ol>
          `
        },
        'analysis-create': {
          title: '创建分析任务',
          content: `
            <h2>创建分析任务</h2>
            <p>分析任务是系统的核心功能，支持多种机器学习算法。</p>
            
            <h3>任务类型</h3>
            <ul>
              <li><strong>回归分析</strong>：预测连续数值</li>
              <li><strong>分类分析</strong>：预测类别标签</li>
              <li><strong>聚类分析</strong>：发现数据模式</li>
            </ul>
            
            <h3>创建步骤</h3>
            <ol>
              <li>选择数据源（数据集或上传文件）</li>
              <li>配置分析参数</li>
              <li>选择算法类型</li>
              <li>提交任务并等待结果</li>
            </ol>
            
            <h3>参数配置建议</h3>
            <ul>
              <li>根据数据特点选择合适的算法</li>
              <li>调整参数以获得最佳性能</li>
              <li>使用系统推荐的参数作为起点</li>
            </ul>
          `
        },
        faq: {
          title: '常见问题',
          content: `
            <h2>常见问题解答</h2>
            
            <h3>数据相关</h3>
            <div class="faq-item">
              <h4>Q: 支持哪些数据格式？</h4>
              <p>A: 系统支持CSV、Excel等格式，文件大小限制为100MB。</p>
            </div>
            
            <div class="faq-item">
              <h4>Q: 数据上传失败怎么办？</h4>
              <p>A: 请检查文件格式、大小和网络连接。确保数据格式正确且无特殊字符。</p>
            </div>
            
            <h3>分析相关</h3>
            <div class="faq-item">
              <h4>Q: 分析任务需要多长时间？</h4>
              <p>A: 取决于数据大小和算法复杂度，通常在几分钟到几小时之间。</p>
            </div>
            
            <div class="faq-item">
              <h4>Q: 如何选择合适的算法？</h4>
              <p>A: 根据问题类型选择：回归问题用回归算法，分类问题用分类算法。系统会提供推荐。</p>
            </div>
            
            <h3>技术支持</h3>
            <div class="faq-item">
              <h4>Q: 遇到技术问题如何获得帮助？</h4>
              <p>A: 可以通过"联系支持"页面提交问题，或发送邮件到技术支持邮箱。</p>
            </div>
          `
        }
      }
    };
  },
  computed: {
    currentTitle() {
      return this.helpContent[this.activeSection]?.title || '帮助中心';
    },
    currentContent() {
      return this.helpContent[this.activeSection]?.content || '<p>内容正在加载中...</p>';
    }
  },
  methods: {
    handleMenuSelect(index) {
      this.activeSection = index;
      this.feedback = null;
      this.feedbackText = '';
    },
    handleSearch() {
      // 实现搜索功能
      console.log('搜索:', this.searchQuery);
    },
    printPage() {
      window.print();
    },
    sharePage() {
      if (navigator.share) {
        navigator.share({
          title: this.currentTitle,
          url: window.location.href
        });
      } else {
        // 复制链接到剪贴板
        navigator.clipboard.writeText(window.location.href);
        this.$message.success('链接已复制到剪贴板');
      }
    },
    submitFeedback(helpful) {
      this.feedback = helpful;
      if (helpful) {
        this.$message.success('感谢您的反馈！');
      }
    },
    submitDetailedFeedback() {
      // 提交详细反馈
      console.log('详细反馈:', this.feedbackText);
      this.$message.success('反馈已提交，感谢您的建议！');
      this.feedbackText = '';
    }
  }
};
</script>

<style scoped>
.help-center {
  height: calc(100vh - 84px);
  background: #f5f7fa;
}

.help-sidebar {
  background: white;
  border-right: 1px solid #e4e7ed;
}

.help-search {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.help-menu {
  border: none;
}

.help-content {
  background: white;
  padding: 0;
}

.help-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  border-bottom: 1px solid #f0f0f0;
}

.help-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.help-body {
  padding: 32px;
  line-height: 1.8;
}

.help-body h2 {
  color: #303133;
  margin-bottom: 16px;
}

.help-body h3 {
  color: #606266;
  margin: 24px 0 12px 0;
}

.help-body ul, .help-body ol {
  padding-left: 24px;
}

.help-body li {
  margin-bottom: 8px;
}

.faq-item {
  margin-bottom: 24px;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 4px;
}

.faq-item h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.faq-item p {
  margin: 0;
  color: #606266;
}

.help-feedback {
  padding: 32px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.feedback-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.feedback-buttons .el-button.active {
  background: #409eff;
  color: white;
}
</style>
