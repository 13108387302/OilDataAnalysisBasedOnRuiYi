<template>
  <div class="prediction-history">
    <el-card>
      <div slot="header" class="clearfix">
        <span>预测历史</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="getList">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </div>

      <!-- 搜索条件 -->
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
        <el-form-item label="预测名称" prop="predictionName">
          <el-input
            v-model="queryParams.predictionName"
            placeholder="请输入预测名称"
            clearable
            @keyup.enter.native="handleQuery"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="等待中" value="PENDING" />
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 预测列表 -->
      <el-table v-loading="loading" :data="predictionList">
        <el-table-column label="预测名称" prop="predictionName" min-width="150" />
        <el-table-column label="模型名称" prop="modelName" min-width="120" />
        <el-table-column label="状态" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusColor(scope.row.status)" size="mini">
              {{ getStatusName(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预测数量" align="center" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.predictionCount || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="执行时间" align="center" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.executionTime ? scope.row.executionTime + 's' : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" width="160">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template slot-scope="scope">
            <el-button 
              size="mini" 
              type="text" 
              @click="handleView(scope.row)"
              v-if="scope.row.status === 'COMPLETED'">
              <i class="el-icon-view"></i> 查看结果
            </el-button>
            <el-button 
              size="mini" 
              type="text" 
              @click="downloadResult(scope.row)"
              v-if="scope.row.status === 'COMPLETED' && scope.row.resultFilePath">
              <i class="el-icon-download"></i> 下载
            </el-button>
            <el-button 
              size="mini" 
              type="text" 
              @click="handleRetry(scope.row)"
              v-if="scope.row.status === 'FAILED'">
              <i class="el-icon-refresh"></i> 重试
            </el-button>
            <el-button 
              size="mini" 
              type="text" 
              @click="handleDelete(scope.row)"
              style="color: #f56c6c">
              <i class="el-icon-delete"></i> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList" />
    </el-card>

    <!-- 预测结果查看对话框 -->
    <el-dialog title="预测结果" :visible.sync="resultDialogVisible" width="80%">
      <div v-if="currentPrediction">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="预测名称">{{ currentPrediction.predictionName }}</el-descriptions-item>
          <el-descriptions-item label="模型名称">{{ currentPrediction.modelName }}</el-descriptions-item>
          <el-descriptions-item label="预测状态">
            <el-tag :type="getStatusColor(currentPrediction.status)">
              {{ getStatusName(currentPrediction.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="执行时间">{{ currentPrediction.executionTime }}秒</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentPrediction.createTime }}</el-descriptions-item>
          <el-descriptions-item label="预测数量">{{ currentPrediction.predictionCount }}</el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentPrediction.status === 'COMPLETED'" style="margin-top: 20px;">
          <h3>预测结果详情</h3>
          <el-card v-loading="resultLoading">
            <div v-if="predictionResult">
              <!-- 预测摘要信息 -->
              <div v-if="predictionResult.summary" style="margin-bottom: 20px;">
                <h4>预测摘要</h4>
                <el-row :gutter="20">
                  <el-col :span="6" v-for="(value, key) in predictionResult.summary" :key="key">
                    <el-statistic :title="key" :value="value" />
                  </el-col>
                </el-row>
              </div>

              <!-- 预测结果表格 -->
              <div>
                <h4>
                  预测结果预览
                  <el-tag
                    :type="predictionResult.taskType === 'classification' ? 'success' : 'primary'"
                    size="small"
                    style="margin-left: 10px;">
                    {{ predictionResult.taskType === 'classification' ? '分类预测' : '回归预测' }}
                  </el-tag>
                </h4>

                <!-- 分类预测结果表格 -->
                <el-table
                  v-if="predictionResult.taskType === 'classification'"
                  :data="predictionResult.previewData"
                  border
                  size="mini"
                  max-height="400">
                  <el-table-column type="index" label="序号" width="60" />
                  <el-table-column prop="预测类别" label="预测类别" min-width="120">
                    <template slot-scope="scope">
                      <el-tag type="success" size="small">{{ scope.row['预测类别'] }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="置信度" label="置信度" width="100">
                    <template slot-scope="scope">
                      <span :style="getConfidenceStyle(scope.row['置信度'])">
                        {{ scope.row['置信度'] }}
                      </span>
                    </template>
                  </el-table-column>
                  <!-- 概率分布列 -->
                  <el-table-column
                    v-for="column in predictionResult.columns.filter(col => col.startsWith('概率'))"
                    :key="column"
                    :prop="column"
                    :label="column"
                    min-width="120">
                    <template slot-scope="scope">
                      <span style="color: #606266;">{{ scope.row[column] }}</span>
                    </template>
                  </el-table-column>
                  <!-- 输入特征列 -->
                  <el-table-column
                    v-for="column in predictionResult.columns.filter(col => !['序号', '预测类别', '置信度'].includes(col) && !col.startsWith('概率'))"
                    :key="column"
                    :prop="column"
                    :label="column"
                    min-width="100">
                    <template slot-scope="scope">
                      <span>{{ scope.row[column] }}</span>
                    </template>
                  </el-table-column>
                </el-table>

                <!-- 回归预测结果表格 -->
                <el-table
                  v-else
                  :data="predictionResult.previewData"
                  border
                  size="mini"
                  max-height="400">
                  <el-table-column type="index" label="序号" width="60" />
                  <el-table-column prop="预测值" label="预测值" min-width="120">
                    <template slot-scope="scope">
                      <span style="font-weight: bold; color: #409EFF;">
                        {{ scope.row['预测值'] }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="置信度" label="置信度" width="100">
                    <template slot-scope="scope">
                      <span :style="getConfidenceStyle(scope.row['置信度'])">
                        {{ scope.row['置信度'] }}
                      </span>
                    </template>
                  </el-table-column>
                  <!-- 输入特征列 -->
                  <el-table-column
                    v-for="column in predictionResult.columns.filter(col => !['序号', '预测值', '置信度'].includes(col))"
                    :key="column"
                    :prop="column"
                    :label="column"
                    min-width="100">
                    <template slot-scope="scope">
                      <span>{{ scope.row[column] }}</span>
                    </template>
                  </el-table-column>
                </el-table>

                <!-- 分类预测的概率分布可视化 -->
                <div v-if="predictionResult.taskType === 'classification' && predictionResult.previewData.length > 0"
                     style="margin-top: 20px;">
                  <h4>样本概率分布示例</h4>
                  <el-row :gutter="20">
                    <el-col :span="12" v-for="(sample, index) in getClassificationSamples()" :key="index">
                      <probability-chart
                        :probabilities="sample.probabilities"
                        :title="`样本 ${sample.index} - ${sample.predictedClass}`"
                        :show-details="false" />
                    </el-col>
                  </el-row>
                </div>

                <div style="margin-top: 10px; text-align: center">
                  <span style="color: #909399">
                    显示前{{ predictionResult.previewData ? predictionResult.previewData.length : 0 }}条记录，
                    共{{ predictionResult.totalCount || predictionResult.previewData.length }}条
                  </span>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div style="margin-top: 15px; text-align: center;">
                <el-button type="info" icon="el-icon-view" @click="viewDetailedResult" style="margin-right: 10px;">
                  查看详细分析
                </el-button>
                <el-button type="primary" icon="el-icon-download" @click="downloadResult(currentPrediction)">
                  下载完整结果
                </el-button>
              </div>
            </div>
            <div v-else style="text-align: center; padding: 40px">
              <i class="el-icon-warning" style="font-size: 48px; color: #E6A23C; margin-bottom: 10px;"></i>
              <div style="color: #909399; font-size: 16px;">暂无预测结果数据</div>
              <div style="color: #C0C4CC; font-size: 14px; margin-top: 5px;">
                可能原因：预测任务未正确完成或结果数据丢失
              </div>
              <el-button type="text" @click="loadPredictionResult(currentPrediction.id)" style="margin-top: 10px;">
                重新加载
              </el-button>
            </div>
          </el-card>
        </div>
        
        <div v-if="currentPrediction.status === 'FAILED'" style="margin-top: 20px;">
          <h3>错误信息</h3>
          <el-alert
            :title="currentPrediction.errorMessage || '预测执行失败'"
            type="error"
            show-icon
            :closable="false">
          </el-alert>
        </div>
      </div>
    </el-dialog>

    <!-- 详细分析对话框 -->
    <el-dialog
      title="预测结果详细分析"
      :visible.sync="showDetailedAnalysis"
      width="90%"
      :close-on-click-modal="false"
      class="detailed-analysis-dialog">
      <div v-loading="analysisLoading">
        <div v-if="!detailedAnalysis" style="text-align: center; padding: 50px;">
          <i class="el-icon-loading" style="font-size: 32px; color: #409EFF;"></i>
          <div style="margin-top: 15px; color: #606266;">正在生成详细分析...</div>
        </div>
        <div v-if="detailedAnalysis">
        <el-tabs type="border-card" v-model="activeAnalysisTab" @tab-click="handleTabChange">
          <!-- 概览 -->
          <el-tab-pane label="分析概览" name="overview">
            <div class="analysis-overview">
              <el-row :gutter="20">
                <el-col :span="6">
                  <el-card shadow="hover" class="overview-card">
                    <div class="overview-item">
                      <div class="overview-icon">
                        <i class="el-icon-data-line" style="color: #409EFF;"></i>
                      </div>
                      <div class="overview-content">
                        <div class="overview-value">{{ detailedAnalysis.basicStats.sampleCount }}</div>
                        <div class="overview-label">预测样本数</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="hover" class="overview-card">
                    <div class="overview-item">
                      <div class="overview-icon">
                        <i class="el-icon-trophy" style="color: #67C23A;"></i>
                      </div>
                      <div class="overview-content">
                        <div class="overview-value">{{ detailedAnalysis.qualityAssessment.avgConfidence }}</div>
                        <div class="overview-label">平均置信度</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="hover" class="overview-card">
                    <div class="overview-item">
                      <div class="overview-icon">
                        <i class="el-icon-location" style="color: #E6A23C;"></i>
                      </div>
                      <div class="overview-content">
                        <div class="overview-value">{{ detailedAnalysis.basicStats.depth.range }}m</div>
                        <div class="overview-label">深度范围</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card shadow="hover" class="overview-card">
                    <div class="overview-item">
                      <div class="overview-icon">
                        <i class="el-icon-medal" style="color: #F56C6C;"></i>
                      </div>
                      <div class="overview-content">
                        <div class="overview-value">{{ getQualityLabel(detailedAnalysis.qualityAssessment.quality) }}</div>
                        <div class="overview-label">预测质量</div>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>

              <!-- 快速洞察 -->
              <el-card shadow="never" style="margin-top: 20px;">
                <div slot="header">
                  <i class="el-icon-view"></i> 快速洞察
                </div>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <div class="insight-item">
                      <h4><i class="el-icon-s-data"></i> 预测分布特征</h4>
                      <p>预测值范围：{{ detailedAnalysis.basicStats.prediction.min }} - {{ detailedAnalysis.basicStats.prediction.max }}</p>
                      <p>标准差：{{ detailedAnalysis.basicStats.prediction.std }}（{{ getPredictionVariabilityLevel() }}）</p>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="insight-item">
                      <h4><i class="el-icon-success"></i> 置信度表现</h4>
                      <p>高置信度样本：{{ detailedAnalysis.confidenceAnalysis.high.count }}个（{{ detailedAnalysis.confidenceAnalysis.high.percentage }}%）</p>
                      <p>需要关注的低置信度样本：{{ detailedAnalysis.confidenceAnalysis.low.count }}个</p>
                    </div>
                  </el-col>
                </el-row>
              </el-card>
            </div>
          </el-tab-pane>

          <!-- 统计分析 -->
          <el-tab-pane label="统计分析" name="statistics">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-data-analysis"></i> 预测值统计
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">样本数量</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.sampleCount }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">最小值</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.prediction.min }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">最大值</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.prediction.max }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">平均值</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.prediction.mean }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">标准差</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.prediction.std }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">变异系数</span>
                    <span class="stat-value">{{ calculateCoefficientOfVariation() }}%</span>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-pie-chart"></i> 置信度统计
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">最小置信度</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.confidence.min }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">最大置信度</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.confidence.max }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">平均置信度</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.confidence.mean }}</span>
                  </div>
                  <div class="confidence-distribution">
                    <div class="confidence-bar">
                      <div class="confidence-segment high"
                           :style="{width: detailedAnalysis.confidenceAnalysis.high.percentage + '%'}">
                        {{ detailedAnalysis.confidenceAnalysis.high.percentage }}%
                      </div>
                      <div class="confidence-segment medium"
                           :style="{width: detailedAnalysis.confidenceAnalysis.medium.percentage + '%'}">
                        {{ detailedAnalysis.confidenceAnalysis.medium.percentage }}%
                      </div>
                      <div class="confidence-segment low"
                           :style="{width: detailedAnalysis.confidenceAnalysis.low.percentage + '%'}">
                        {{ detailedAnalysis.confidenceAnalysis.low.percentage }}%
                      </div>
                    </div>
                    <div class="confidence-legend">
                      <span class="legend-item high">高(≥0.8)</span>
                      <span class="legend-item medium">中(0.6-0.8)</span>
                      <span class="legend-item low">低(<0.6)</span>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-position"></i> 深度统计
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">最小深度</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.depth.min }}m</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">最大深度</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.depth.max }}m</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">深度范围</span>
                    <span class="stat-value">{{ detailedAnalysis.basicStats.depth.range }}m</span>
                  </div>
                  <div class="depth-distribution">
                    <div class="depth-segment">
                      <span class="depth-label">浅层</span>
                      <span class="depth-range">{{ detailedAnalysis.depthAnalysis.shallow.range }}</span>
                      <span class="depth-count">{{ detailedAnalysis.depthAnalysis.shallow.count }}个</span>
                    </div>
                    <div class="depth-segment">
                      <span class="depth-label">中层</span>
                      <span class="depth-range">{{ detailedAnalysis.depthAnalysis.medium.range }}</span>
                      <span class="depth-count">{{ detailedAnalysis.depthAnalysis.medium.count }}个</span>
                    </div>
                    <div class="depth-segment">
                      <span class="depth-label">深层</span>
                      <span class="depth-range">{{ detailedAnalysis.depthAnalysis.deep.range }}</span>
                      <span class="depth-count">{{ detailedAnalysis.depthAnalysis.deep.count }}个</span>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- 可视化图表 -->
          <el-tab-pane label="可视化图表" name="charts">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-data-line"></i> 预测值分布
                  </div>
                  <div id="predictionDistributionChart" style="height: 300px;"></div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-pie-chart"></i> 置信度分布
                  </div>
                  <div id="confidenceDistributionChart" style="height: 300px;"></div>
                </el-card>
              </el-col>
            </el-row>
            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-s-marketing"></i> 深度-预测值关系
                  </div>
                  <div id="depthPredictionChart" style="height: 300px;"></div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-connection"></i> 置信度-预测值关系
                  </div>
                  <div id="confidencePredictionChart" style="height: 300px;"></div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- 数据质量 -->
          <el-tab-pane label="数据质量" name="quality">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-medal"></i> 质量评估
                  </div>
                  <div class="quality-assessment-detailed">
                    <div class="quality-score">
                      <el-progress
                        type="circle"
                        :percentage="getQualityScore()"
                        :color="getQualityColor()"
                        :width="120">
                        <template slot="default">
                          <span class="quality-score-text">{{ getQualityScore() }}</span>
                          <span class="quality-score-label">质量分</span>
                        </template>
                      </el-progress>
                    </div>
                    <div class="quality-details">
                      <div class="quality-item">
                        <span class="quality-metric">预测质量等级</span>
                        <el-tag :type="getQualityTagType(detailedAnalysis.qualityAssessment.quality)" size="medium">
                          {{ getQualityLabel(detailedAnalysis.qualityAssessment.quality) }}
                        </el-tag>
                      </div>
                      <div class="quality-item">
                        <span class="quality-metric">平均置信度</span>
                        <span class="quality-value">{{ detailedAnalysis.qualityAssessment.avgConfidence }}</span>
                      </div>
                      <div class="quality-item">
                        <span class="quality-metric">高置信度占比</span>
                        <span class="quality-value">{{ detailedAnalysis.confidenceAnalysis.high.percentage }}%</span>
                      </div>
                      <div class="quality-item">
                        <span class="quality-metric">数据一致性</span>
                        <span class="quality-value">{{ getDataConsistency() }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="quality-description">
                    <p><i class="el-icon-info"></i> {{ detailedAnalysis.qualityAssessment.description }}</p>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-warning"></i> 异常检测
                  </div>
                  <div class="anomaly-detection">
                    <div class="anomaly-item">
                      <h4>预测值异常</h4>
                      <p v-if="detailedAnalysis.predictionDistribution.outliers.length === 0">
                        <i class="el-icon-success" style="color: #67C23A;"></i> 未检测到异常值
                      </p>
                      <div v-else>
                        <p><i class="el-icon-warning" style="color: #E6A23C;"></i> 检测到 {{ detailedAnalysis.predictionDistribution.outliers.length }} 个异常值</p>
                        <el-tag v-for="outlier in detailedAnalysis.predictionDistribution.outliers.slice(0, 5)"
                                :key="outlier" size="mini" type="warning" style="margin: 2px;">
                          {{ outlier }}
                        </el-tag>
                        <span v-if="detailedAnalysis.predictionDistribution.outliers.length > 5">...</span>
                      </div>
                    </div>
                    <div class="anomaly-item">
                      <h4>置信度异常</h4>
                      <p v-if="detailedAnalysis.confidenceAnalysis.low.count === 0">
                        <i class="el-icon-success" style="color: #67C23A;"></i> 所有预测置信度良好
                      </p>
                      <p v-else>
                        <i class="el-icon-warning" style="color: #E6A23C;"></i>
                        {{ detailedAnalysis.confidenceAnalysis.low.count }} 个低置信度预测需要关注
                      </p>
                    </div>
                    <div class="anomaly-item">
                      <h4>数据完整性</h4>
                      <p><i class="el-icon-success" style="color: #67C23A;"></i> 数据完整，无缺失值</p>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- 专业分析 -->
          <el-tab-pane label="专业分析" name="professional">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-cpu"></i> 地质解释
                  </div>
                  <div class="geological-interpretation">
                    <div class="interpretation-item">
                      <h4>深度分层特征</h4>
                      <div class="depth-layers">
                        <div class="layer-item">
                          <span class="layer-name">浅层 ({{ detailedAnalysis.depthAnalysis.shallow.range }})</span>
                          <span class="layer-prediction">平均GR: {{ getLayerAveragePrediction('shallow') }}</span>
                          <span class="layer-interpretation">{{ getLayerInterpretation('shallow') }}</span>
                        </div>
                        <div class="layer-item">
                          <span class="layer-name">中层 ({{ detailedAnalysis.depthAnalysis.medium.range }})</span>
                          <span class="layer-prediction">平均GR: {{ getLayerAveragePrediction('medium') }}</span>
                          <span class="layer-interpretation">{{ getLayerInterpretation('medium') }}</span>
                        </div>
                        <div class="layer-item">
                          <span class="layer-name">深层 ({{ detailedAnalysis.depthAnalysis.deep.range }})</span>
                          <span class="layer-prediction">平均GR: {{ getLayerAveragePrediction('deep') }}</span>
                          <span class="layer-interpretation">{{ getLayerInterpretation('deep') }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="interpretation-item">
                      <h4>岩性预测</h4>
                      <p>{{ getLithologyPrediction() }}</p>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-data-board"></i> 统计指标
                  </div>
                  <div class="statistical-metrics">
                    <div class="metric-item">
                      <span class="metric-name">四分位距 (IQR)</span>
                      <span class="metric-value">{{ calculateIQR() }}</span>
                    </div>
                    <div class="metric-item">
                      <span class="metric-name">偏度 (Skewness)</span>
                      <span class="metric-value">{{ calculateSkewness() }}</span>
                    </div>
                    <div class="metric-item">
                      <span class="metric-name">峰度 (Kurtosis)</span>
                      <span class="metric-value">{{ calculateKurtosis() }}</span>
                    </div>
                    <div class="metric-item">
                      <span class="metric-name">置信度标准差</span>
                      <span class="metric-value">{{ calculateConfidenceStd() }}</span>
                    </div>
                    <div class="metric-item">
                      <span class="metric-name">预测稳定性</span>
                      <span class="metric-value">{{ getPredictionStability() }}</span>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- 优化建议 -->
          <el-tab-pane label="优化建议" name="recommendations">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-magic-stick"></i> 模型优化建议
                  </div>
                  <div class="recommendations-section">
                    <div class="recommendation-category">
                      <h4><i class="el-icon-setting"></i> 模型调优</h4>
                      <ul class="recommendation-list">
                        <li v-for="rec in getModelOptimizationRecommendations()" :key="rec">
                          <i class="el-icon-right"></i> {{ rec }}
                        </li>
                      </ul>
                    </div>
                    <div class="recommendation-category">
                      <h4><i class="el-icon-document"></i> 数据改进</h4>
                      <ul class="recommendation-list">
                        <li v-for="rec in getDataImprovementRecommendations()" :key="rec">
                          <i class="el-icon-right"></i> {{ rec }}
                        </li>
                      </ul>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="never">
                  <div slot="header">
                    <i class="el-icon-guide"></i> 应用建议
                  </div>
                  <div class="recommendations-section">
                    <div class="recommendation-category">
                      <h4><i class="el-icon-view"></i> 结果应用</h4>
                      <ul class="recommendation-list">
                        <li v-for="rec in getApplicationRecommendations()" :key="rec">
                          <i class="el-icon-right"></i> {{ rec }}
                        </li>
                      </ul>
                    </div>
                    <div class="recommendation-category">
                      <h4><i class="el-icon-warning"></i> 注意事项</h4>
                      <ul class="recommendation-list warning">
                        <li v-for="warning in getWarnings()" :key="warning">
                          <i class="el-icon-warning"></i> {{ warning }}
                        </li>
                      </ul>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showDetailedAnalysis = false">关闭</el-button>
        <el-button type="info" @click="printAnalysisReport">打印报告</el-button>
        <el-button type="primary" @click="exportDetailedAnalysis">导出分析报告</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPrediction, delPrediction, getPredictionResult } from '@/api/petrol/prediction'
import { parseTime } from '@/utils/ruoyi'
import ProbabilityChart from '@/components/ProbabilityChart'
import * as echarts from 'echarts'

export default {
  name: 'PredictionHistory',
  components: {
    ProbabilityChart
  },
  components: {
    Pagination: () => import('@/components/Pagination')
  },
  data() {
    return {
      loading: false,
      predictionList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        predictionName: null,
        status: null
      },
      
      // 结果查看对话框
      resultDialogVisible: false,
      resultLoading: false,
      currentPrediction: null,
      predictionResult: null,

      // 详细分析对话框
      showDetailedAnalysis: false,
      detailedAnalysis: null,
      activeAnalysisTab: 'overview',
      analysisLoading: false
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 获取预测列表 */
    getList() {
      this.loading = true

      listPrediction(this.queryParams).then(response => {
        this.predictionList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(error => {
        console.error('获取预测列表失败:', error)
        // 如果API调用失败，显示空列表
        this.predictionList = []
        this.total = 0
        this.loading = false
        this.$message.error('无法连接到服务器，请检查网络连接')
      })
    },

    /** 搜索 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    /** 重置搜索 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },

    /** 查看结果 */
    handleView(prediction) {
      this.currentPrediction = prediction
      this.predictionResult = null
      this.resultDialogVisible = true
      
      if (prediction.status === 'COMPLETED') {
        this.loadPredictionResult(prediction.id)
      }
    },

    /** 加载预测结果 */
    loadPredictionResult(predictionId) {
      this.resultLoading = true
      console.log('开始加载预测结果，ID:', predictionId)

      getPredictionResult(predictionId).then(response => {
        console.log('预测结果API原始响应:', response)

        if (response.code === 200) {
          if (response.data) {
            // 解析预测结果数据
            let resultData = response.data.data || response.data

            // 如果是字符串，尝试解析JSON
            if (typeof resultData === 'string') {
              try {
                resultData = JSON.parse(resultData)
              } catch (e) {
                console.error('解析预测结果JSON失败:', e)
                this.$message.error('预测结果数据格式错误')
                this.resultLoading = false
                return
              }
            }

            // 处理预测结果数据格式
            this.predictionResult = this.formatPredictionResult(resultData)
            console.log('格式化后的预测结果:', this.predictionResult)

            // 验证数据完整性
            if (this.predictionResult && this.predictionResult.previewData && this.predictionResult.previewData.length > 0) {
              console.log('预测结果加载成功，数据条数:', this.predictionResult.previewData.length)
              this.$message.success('预测结果加载成功')
            } else {
              console.warn('预测结果数据为空或格式不正确')
              this.$message.warning('预测结果数据为空，可能需要重新执行预测')
            }
          } else {
            console.error('API响应成功但数据为空:', response)
            this.predictionResult = null
            this.$message.error('服务器返回空数据')
          }
        } else {
          console.error('预测结果API返回错误码:', response.code, '错误信息:', response.msg)
          this.predictionResult = null

          // 根据错误码提供具体的错误信息
          let errorMessage = response.msg || '获取预测结果失败'
          if (response.code === 500) {
            errorMessage += ' (服务器内部错误，请检查后端日志)'
          } else if (response.code === 404) {
            errorMessage += ' (预测记录不存在)'
          }

          this.$message.error(errorMessage)
        }

        this.resultLoading = false
      }).catch(error => {
        console.error('获取预测结果网络错误:', error)
        this.resultLoading = false
        this.predictionResult = null

        // 提供详细的错误信息
        let errorMessage = '获取预测结果失败: '
        if (error.response) {
          // 服务器响应了错误状态码
          errorMessage += `HTTP ${error.response.status} - ${error.response.statusText}`
          console.error('错误响应数据:', error.response.data)
        } else if (error.request) {
          // 请求已发出但没有收到响应
          errorMessage += '网络连接失败，请检查网络或后端服务状态'
        } else {
          // 其他错误
          errorMessage += error.message || '未知错误'
        }

        this.$message.error(errorMessage)

        // 显示调试建议
        this.$notify({
          title: '调试建议',
          message: '请检查：1) 后端服务是否运行 2) 预测记录是否存在 3) 数据库连接是否正常',
          type: 'info',
          duration: 8000
        })
      })
    },

    /** 下载结果 */
    downloadResult(prediction) {
      console.log('开始下载预测结果:', prediction)

      if (prediction.resultFilePath) {
        // 使用后端下载接口
        const downloadUrl = process.env.VUE_APP_BASE_API + '/petrol/prediction/download/' + prediction.id
        console.log('下载URL:', downloadUrl)

        // 创建隐藏的下载链接
        const link = document.createElement('a')
        link.href = downloadUrl
        link.download = `prediction_result_${prediction.id}.xlsx`
        link.style.display = 'none'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)

        this.$message.success('开始下载预测结果文件')
      } else if (this.predictionResult && this.predictionResult.previewData) {
        // 如果没有文件路径但有预测结果数据，生成CSV下载
        this.downloadPredictionAsCSV(prediction)
      } else {
        this.$message.warning('暂无可下载的结果文件，请确认预测任务已完成')
      }
    },

    /** 下载预测结果为CSV */
    downloadPredictionAsCSV(prediction) {
      try {
        if (!this.predictionResult || !this.predictionResult.previewData) {
          this.$message.error('没有可下载的预测数据')
          return
        }

        // 转换数据为CSV格式
        const csvContent = this.convertToCSV(this.predictionResult.previewData)

        // 创建Blob对象
        const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })

        // 创建下载链接
        const link = document.createElement('a')
        const url = URL.createObjectURL(blob)
        link.setAttribute('href', url)
        link.setAttribute('download', `prediction_result_${prediction.id}_${Date.now()}.csv`)
        link.style.visibility = 'hidden'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)

        this.$message.success('预测结果已导出为CSV文件')
      } catch (error) {
        console.error('导出CSV失败:', error)
        this.$message.error('导出失败: ' + error.message)
      }
    },

    /** 转换数据为CSV格式 */
    convertToCSV(data) {
      if (!data || data.length === 0) {
        return ''
      }

      // 获取表头
      const headers = Object.keys(data[0])
      const csvHeaders = headers.join(',')

      // 转换数据行
      const csvRows = data.map(row => {
        return headers.map(header => {
          const value = row[header]
          // 处理包含逗号或引号的值
          if (typeof value === 'string' && (value.includes(',') || value.includes('"'))) {
            return `"${value.replace(/"/g, '""')}"`
          }
          return value
        }).join(',')
      })

      return [csvHeaders, ...csvRows].join('\n')
    },

    /** 重试预测 */
    handleRetry(prediction) {
      this.$confirm('确认重新执行该预测任务？', '提示', { type: 'warning' }).then(() => {
        this.$message.info('重试功能开发中')
      })
    },

    /** 删除预测 */
    handleDelete(prediction) {
      this.$confirm('确认删除该预测记录？', '提示', { type: 'warning' }).then(() => {
        delPrediction(prediction.id).then(() => {
          this.$message.success('删除成功')
          this.getList()
          this.$emit('refresh')
        })
      })
    },

    /** 获取状态颜色 */
    getStatusColor(status) {
      const colorMap = {
        'PENDING': 'info',
        'RUNNING': 'warning',
        'COMPLETED': 'success',
        'FAILED': 'danger'
      }
      return colorMap[status] || 'info'
    },

    /** 获取状态名称 */
    getStatusName(status) {
      const nameMap = {
        'PENDING': '等待中',
        'RUNNING': '运行中',
        'COMPLETED': '已完成',
        'FAILED': '失败'
      }
      return nameMap[status] || status
    },

    /** 格式化预测结果数据 */
    formatPredictionResult(rawData) {
      if (!rawData) return null

      try {
        console.log('🔍 formatPredictionResult 输入数据:', rawData)

        // 如果数据包含predictions数组，转换为表格格式
        if (rawData.predictions && Array.isArray(rawData.predictions)) {
          const predictions = rawData.predictions
          const inputData = rawData.input_data || []
          const confidences = rawData.confidences || []
          const statistics = rawData.statistics || {}

          console.log('📊 预测数据解析:', {
            predictions: predictions.length,
            inputData: inputData.length,
            confidences: confidences.length,
            statistics
          })

          // 判断任务类型
          const isClassification = statistics.task_type === 'classification' ||
                                 (predictions.length > 0 && typeof predictions[0] === 'object' && predictions[0].predicted_class)

          console.log('🎯 任务类型判断:', isClassification ? '分类' : '回归')

          let previewData = []
          let columns = []
          let summary = {}

          if (isClassification) {
            // 分类任务结果格式化
            previewData = predictions.map((prediction, index) => {
              const row = {
                '序号': index + 1,
                '预测类别': prediction.predicted_class || prediction,
                '置信度': prediction.confidence ?
                  (prediction.confidence * 100).toFixed(2) + '%' :
                  (confidences[index] ? (confidences[index] * 100).toFixed(2) + '%' : '-')
              }

              // 添加概率分布（只显示前3个最高概率）
              if (prediction.probabilities) {
                const sortedProbs = Object.entries(prediction.probabilities)
                  .sort(([,a], [,b]) => b - a)
                  .slice(0, 3)

                sortedProbs.forEach(([className, prob], idx) => {
                  row[`概率${idx + 1}(${className})`] = (prob * 100).toFixed(2) + '%'
                })
              }

              // 添加输入特征
              if (inputData[index]) {
                Object.keys(inputData[index]).forEach(key => {
                  row[key] = inputData[index][key]
                })
              }

              return row
            })

            // 构建分类任务摘要
            summary = {
              '预测数量': predictions.length,
              '任务类型': '分类预测',
              '类别数量': statistics.unique_classes || '-',
              '最常见类别': statistics.most_common_class || '-',
              '平均置信度': statistics.mean_confidence ?
                (statistics.mean_confidence * 100).toFixed(2) + '%' : '-'
            }

            // 添加类别分布
            if (statistics.class_percentages) {
              Object.entries(statistics.class_percentages).forEach(([className, percentage]) => {
                summary[`${className}占比`] = percentage.toFixed(1) + '%'
              })
            }

          } else {
            // 回归任务结果格式化
            console.log('📈 开始处理回归任务数据')
            previewData = predictions.map((prediction, index) => {
              const row = {
                '序号': index + 1,
                '预测值': typeof prediction === 'number' ? prediction.toFixed(4) : prediction
              }

              // 添加置信度
              if (confidences[index] !== undefined) {
                row['置信度'] = (confidences[index] * 100).toFixed(2) + '%'
              }

              // 添加输入特征
              if (inputData[index]) {
                Object.keys(inputData[index]).forEach(key => {
                  row[key] = inputData[index][key]
                })
              }

              return row
            })

            console.log('📈 回归任务预览数据生成完成，条数:', previewData.length)

            // 构建回归任务摘要
            summary = {
              '预测数量': predictions.length,
              '任务类型': '回归预测',
              '平均预测值': statistics.mean_prediction ? statistics.mean_prediction.toFixed(4) : '-',
              '最大预测值': statistics.max_prediction ? statistics.max_prediction.toFixed(4) : '-',
              '最小预测值': statistics.min_prediction ? statistics.min_prediction.toFixed(4) : '-',
              '标准差': statistics.std_prediction ? statistics.std_prediction.toFixed(4) : '-',
              '平均置信度': statistics.mean_confidence ?
                (statistics.mean_confidence * 100).toFixed(2) + '%' : '-'
            }
          }

          // 构建列名
          columns = previewData.length > 0 ? Object.keys(previewData[0]) : []

          const result = {
            previewData,
            columns,
            summary,
            rawData,
            taskType: isClassification ? 'classification' : 'regression'
          }

          console.log('✅ formatPredictionResult 返回结果:', {
            previewDataLength: result.previewData.length,
            columnsLength: result.columns.length,
            taskType: result.taskType,
            summary: result.summary
          })

          return result
        }

        // 如果是其他格式，直接返回
        return {
          previewData: rawData.previewData || [],
          columns: rawData.columns || [],
          summary: rawData.summary || {},
          rawData
        }
      } catch (error) {
        console.error('格式化预测结果失败:', error)
        return null
      }
    },

    /** 获取置信度样式 */
    getConfidenceStyle(confidence) {
      const value = parseFloat(confidence)
      if (value >= 0.8) {
        return { color: '#67C23A', fontWeight: 'bold' } // 绿色 - 高置信度
      } else if (value >= 0.6) {
        return { color: '#E6A23C', fontWeight: 'bold' } // 橙色 - 中等置信度
      } else {
        return { color: '#F56C6C', fontWeight: 'bold' } // 红色 - 低置信度
      }
    },

    /** 获取分类预测的样本示例（用于概率分布可视化） */
    getClassificationSamples() {
      if (!this.predictionResult || !this.predictionResult.rawData || !this.predictionResult.rawData.predictions) {
        return []
      }

      const predictions = this.predictionResult.rawData.predictions
      const samples = []

      // 选择前4个样本进行展示
      for (let i = 0; i < Math.min(4, predictions.length); i++) {
        const prediction = predictions[i]
        if (prediction && prediction.probabilities) {
          samples.push({
            index: i + 1,
            predictedClass: prediction.predicted_class,
            probabilities: prediction.probabilities
          })
        }
      }

      return samples
    },

    /** 查看详细分析结果 */
    async viewDetailedResult() {
      if (!this.currentPrediction) {
        this.$message.error('请先选择一个预测任务')
        return
      }

      if (!this.predictionResult || !this.predictionResult.previewData) {
        this.$message.warning('暂无预测结果数据，无法进行详细分析')
        return
      }

      try {
        this.analysisLoading = true
        this.showDetailedAnalysis = true
        this.activeAnalysisTab = 'overview'

        // 生成详细分析数据
        this.generateDetailedAnalysis()

        // 等待DOM更新后渲染图表
        this.$nextTick(() => {
          this.renderAnalysisCharts()
        })

      } catch (error) {
        console.error('打开详细分析失败:', error)
        this.$message.error('打开详细分析失败')
      } finally {
        this.analysisLoading = false
      }
    },

    /** 生成详细分析 */
    generateDetailedAnalysis() {
      if (!this.predictionResult) return

      const data = this.predictionResult.previewData || []
      const summary = this.predictionResult.summary || {}

      // 计算详细统计信息
      this.detailedAnalysis = {
        basicStats: this.calculateBasicStats(data),
        confidenceAnalysis: this.analyzeConfidence(data),
        depthAnalysis: this.analyzeDepth(data),
        predictionDistribution: this.analyzePredictionDistribution(data),
        qualityAssessment: this.assessPredictionQuality(data),
        recommendations: this.generateRecommendations(data, summary)
      }
    },

    /** 计算基础统计 */
    calculateBasicStats(data) {
      if (!data || data.length === 0) return {}

      const predictions = data.map(row => parseFloat(row['预测值']) || 0).filter(v => !isNaN(v))
      const confidences = data.map(row => parseFloat(row['置信度']) || 0).filter(v => !isNaN(v))
      const depths = data.map(row => parseFloat(row['DEPTH']) || 0).filter(v => !isNaN(v))

      return {
        sampleCount: data.length,
        prediction: {
          min: Math.min(...predictions).toFixed(4),
          max: Math.max(...predictions).toFixed(4),
          mean: (predictions.reduce((a, b) => a + b, 0) / predictions.length).toFixed(4),
          std: this.calculateStandardDeviation(predictions).toFixed(4)
        },
        confidence: {
          min: Math.min(...confidences).toFixed(3),
          max: Math.max(...confidences).toFixed(3),
          mean: (confidences.reduce((a, b) => a + b, 0) / confidences.length).toFixed(3)
        },
        depth: {
          min: Math.min(...depths).toFixed(2),
          max: Math.max(...depths).toFixed(2),
          range: (Math.max(...depths) - Math.min(...depths)).toFixed(2)
        }
      }
    },

    /** 分析置信度分布 */
    analyzeConfidence(data) {
      const confidences = data.map(row => parseFloat(row['置信度']) || 0).filter(v => !isNaN(v))

      const high = confidences.filter(c => c >= 0.8).length
      const medium = confidences.filter(c => c >= 0.6 && c < 0.8).length
      const low = confidences.filter(c => c < 0.6).length

      return {
        high: { count: high, percentage: ((high / confidences.length) * 100).toFixed(1) },
        medium: { count: medium, percentage: ((medium / confidences.length) * 100).toFixed(1) },
        low: { count: low, percentage: ((low / confidences.length) * 100).toFixed(1) }
      }
    },

    /** 分析深度分布 */
    analyzeDepth(data) {
      if (!data || data.length === 0) return {}

      const depths = data.map(row => parseFloat(row['DEPTH']) || 0).filter(v => !isNaN(v))
      const predictions = data.map(row => parseFloat(row['预测值']) || 0).filter(v => !isNaN(v))

      if (depths.length === 0 || predictions.length === 0) return {}

      const minDepth = Math.min(...depths)
      const maxDepth = Math.max(...depths)
      const range = maxDepth - minDepth

      // 分为三个深度段，并计算每段的平均预测值
      const shallowData = data.filter(row => {
        const depth = parseFloat(row['DEPTH']) || 0
        return depth < minDepth + range / 3
      })
      const mediumData = data.filter(row => {
        const depth = parseFloat(row['DEPTH']) || 0
        return depth >= minDepth + range / 3 && depth < minDepth + 2 * range / 3
      })
      const deepData = data.filter(row => {
        const depth = parseFloat(row['DEPTH']) || 0
        return depth >= minDepth + 2 * range / 3
      })

      // 计算各层平均预测值
      const calculateLayerAvg = (layerData) => {
        if (layerData.length === 0) return 'N/A'
        const layerPredictions = layerData.map(row => parseFloat(row['预测值']) || 0).filter(v => !isNaN(v))
        if (layerPredictions.length === 0) return 'N/A'
        const avg = layerPredictions.reduce((a, b) => a + b, 0) / layerPredictions.length
        return avg.toFixed(2)
      }

      return {
        shallow: {
          count: shallowData.length,
          range: `${minDepth.toFixed(0)}-${(minDepth + range / 3).toFixed(0)}m`,
          avgPrediction: calculateLayerAvg(shallowData)
        },
        medium: {
          count: mediumData.length,
          range: `${(minDepth + range / 3).toFixed(0)}-${(minDepth + 2 * range / 3).toFixed(0)}m`,
          avgPrediction: calculateLayerAvg(mediumData)
        },
        deep: {
          count: deepData.length,
          range: `${(minDepth + 2 * range / 3).toFixed(0)}-${maxDepth.toFixed(0)}m`,
          avgPrediction: calculateLayerAvg(deepData)
        }
      }
    },

    /** 分析预测值分布 */
    analyzePredictionDistribution(data) {
      const predictions = data.map(row => parseFloat(row['预测值']) || 0).filter(v => !isNaN(v))
      const sorted = [...predictions].sort((a, b) => a - b)

      return {
        quartiles: {
          q1: sorted[Math.floor(sorted.length * 0.25)].toFixed(4),
          q2: sorted[Math.floor(sorted.length * 0.5)].toFixed(4),
          q3: sorted[Math.floor(sorted.length * 0.75)].toFixed(4)
        },
        outliers: this.detectOutliers(predictions)
      }
    },

    /** 评估预测质量 */
    assessPredictionQuality(data) {
      const confidences = data.map(row => parseFloat(row['置信度']) || 0).filter(v => !isNaN(v))
      const avgConfidence = confidences.reduce((a, b) => a + b, 0) / confidences.length

      let quality = 'unknown'
      let description = ''

      if (avgConfidence >= 0.85) {
        quality = 'excellent'
        description = '预测质量优秀，结果高度可信'
      } else if (avgConfidence >= 0.75) {
        quality = 'good'
        description = '预测质量良好，结果较为可信'
      } else if (avgConfidence >= 0.65) {
        quality = 'fair'
        description = '预测质量一般，建议谨慎使用'
      } else {
        quality = 'poor'
        description = '预测质量较差，建议重新训练模型'
      }

      return { quality, description, avgConfidence: avgConfidence.toFixed(3) }
    },

    /** 生成建议 */
    generateRecommendations(data, summary) {
      const recommendations = []
      const confidences = data.map(row => parseFloat(row['置信度']) || 0).filter(v => !isNaN(v))
      const avgConfidence = confidences.reduce((a, b) => a + b, 0) / confidences.length

      if (avgConfidence < 0.7) {
        recommendations.push('平均置信度较低，建议收集更多训练数据或调整模型参数')
      }

      const lowConfidenceCount = confidences.filter(c => c < 0.6).length
      if (lowConfidenceCount > data.length * 0.2) {
        recommendations.push(`有${lowConfidenceCount}个低置信度预测，建议对这些样本进行实地验证`)
      }

      if (data.length < 50) {
        recommendations.push('预测样本数量较少，建议增加样本数量以获得更全面的分析')
      }

      recommendations.push('建议定期更新模型以保持预测准确性')
      recommendations.push('结合地质专业知识对预测结果进行综合分析')

      return recommendations
    },

    /** 计算标准差 */
    calculateStandardDeviation(values) {
      const mean = values.reduce((a, b) => a + b, 0) / values.length
      const squaredDiffs = values.map(value => Math.pow(value - mean, 2))
      const avgSquaredDiff = squaredDiffs.reduce((a, b) => a + b, 0) / values.length
      return Math.sqrt(avgSquaredDiff)
    },

    /** 检测异常值 */
    detectOutliers(values) {
      const sorted = [...values].sort((a, b) => a - b)
      const q1 = sorted[Math.floor(sorted.length * 0.25)]
      const q3 = sorted[Math.floor(sorted.length * 0.75)]
      const iqr = q3 - q1
      const lowerBound = q1 - 1.5 * iqr
      const upperBound = q3 + 1.5 * iqr

      // 返回异常值数组，而不是数量
      return values.filter(v => v < lowerBound || v > upperBound)
    },

    /** 获取质量标签类型 */
    getQualityTagType(quality) {
      const typeMap = {
        'excellent': 'success',
        'good': 'primary',
        'fair': 'warning',
        'poor': 'danger'
      }
      return typeMap[quality] || 'info'
    },

    /** 获取质量标签文本 */
    getQualityLabel(quality) {
      const labelMap = {
        'excellent': '优秀',
        'good': '良好',
        'fair': '一般',
        'poor': '较差'
      }
      return labelMap[quality] || '未知'
    },

    /** 导出详细分析报告 */
    exportDetailedAnalysis() {
      if (!this.detailedAnalysis || !this.currentPrediction) {
        this.$message.error('没有可导出的分析数据')
        return
      }

      try {
        // 生成分析报告内容
        const reportContent = this.generateAnalysisReport()

        // 创建Blob对象
        const blob = new Blob([reportContent], { type: 'text/plain;charset=utf-8;' })

        // 创建下载链接
        const link = document.createElement('a')
        const url = URL.createObjectURL(blob)
        link.setAttribute('href', url)
        link.setAttribute('download', `prediction_analysis_${this.currentPrediction.id}_${Date.now()}.txt`)
        link.style.visibility = 'hidden'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)

        this.$message.success('分析报告已导出')
      } catch (error) {
        console.error('导出分析报告失败:', error)
        this.$message.error('导出失败: ' + error.message)
      }
    },

    /** 生成分析报告内容 */
    generateAnalysisReport() {
      const analysis = this.detailedAnalysis
      const prediction = this.currentPrediction

      return `
石油预测系统 - 详细分析报告
=====================================

预测任务信息:
- 任务名称: ${prediction.predictionName}
- 任务ID: ${prediction.id}
- 创建时间: ${prediction.createTime}
- 执行时间: ${prediction.executionTime}秒
- 预测状态: ${prediction.status}

基础统计信息:
- 样本数量: ${analysis.basicStats.sampleCount}
- 预测值范围: ${analysis.basicStats.prediction.min} - ${analysis.basicStats.prediction.max}
- 预测值平均: ${analysis.basicStats.prediction.mean}
- 预测值标准差: ${analysis.basicStats.prediction.std}
- 置信度范围: ${analysis.basicStats.confidence.min} - ${analysis.basicStats.confidence.max}
- 平均置信度: ${analysis.basicStats.confidence.mean}
- 深度范围: ${analysis.basicStats.depth.min}m - ${analysis.basicStats.depth.max}m

置信度分布:
- 高置信度(≥0.8): ${analysis.confidenceAnalysis.high.count}个 (${analysis.confidenceAnalysis.high.percentage}%)
- 中等置信度(0.6-0.8): ${analysis.confidenceAnalysis.medium.count}个 (${analysis.confidenceAnalysis.medium.percentage}%)
- 低置信度(<0.6): ${analysis.confidenceAnalysis.low.count}个 (${analysis.confidenceAnalysis.low.percentage}%)

质量评估:
- 整体质量: ${this.getQualityLabel(analysis.qualityAssessment.quality)}
- 平均置信度: ${analysis.qualityAssessment.avgConfidence}
- 评估说明: ${analysis.qualityAssessment.description}

优化建议:
${analysis.recommendations.map((rec, index) => `${index + 1}. ${rec}`).join('\n')}

报告生成时间: ${new Date().toLocaleString()}
=====================================
      `.trim()
    },

    /** 刷新（从外部调用） */
    refresh() {
      this.getList()
    },

    // ==================== 详细分析功能 ====================

    /** 查看详细分析 */
    async viewDetailedAnalysis(row) {
      try {
        this.analysisLoading = true
        this.showDetailedAnalysis = true
        this.activeAnalysisTab = 'overview'
        this.currentPrediction = row

        // 获取预测结果详情
        const response = await getPredictionResult(row.id)
        this.predictionResult = response.data

        // 生成详细分析数据
        this.generateDetailedAnalysis()

        // 等待DOM更新后渲染图表
        this.$nextTick(() => {
          this.renderAnalysisCharts()
        })

      } catch (error) {
        console.error('获取详细分析失败:', error)
        this.$modal.msgError('获取详细分析失败')
      } finally {
        this.analysisLoading = false
      }
    },

    /** 渲染分析图表 */
    renderAnalysisCharts() {
      if (!this.detailedAnalysis) return

      this.$nextTick(() => {
        if (this.activeAnalysisTab === 'charts') {
          this.renderPredictionDistributionChart()
          this.renderConfidenceDistributionChart()
          this.renderDepthPredictionChart()
          this.renderConfidencePredictionChart()
        }
      })
    },

    /** 渲染预测值分布图 */
    renderPredictionDistributionChart() {
      const chartElement = document.getElementById('predictionDistributionChart')
      if (!chartElement || !this.predictionResult || !this.predictionResult.previewData) return

      const chart = echarts.init(chartElement)

      // 获取预测值数据
      const data = this.predictionResult.previewData
      const predictions = data.map(row => parseFloat(row['预测值']) || 0).filter(v => !isNaN(v))

      if (predictions.length === 0) {
        chartElement.innerHTML = '<div style="text-align: center; padding: 50px; color: #999;">暂无数据</div>'
        return
      }

      // 计算直方图数据
      const min = Math.min(...predictions)
      const max = Math.max(...predictions)
      const binCount = Math.min(20, Math.max(5, Math.floor(predictions.length / 5))) // 动态计算分组数
      const binWidth = (max - min) / binCount

      const bins = Array(binCount).fill(0)
      const binLabels = []

      for (let i = 0; i < binCount; i++) {
        const binStart = min + i * binWidth
        const binEnd = min + (i + 1) * binWidth
        binLabels.push(`${binStart.toFixed(1)}-${binEnd.toFixed(1)}`)
      }

      predictions.forEach(value => {
        const binIndex = Math.min(Math.floor((value - min) / binWidth), binCount - 1)
        bins[binIndex]++
      })

      const option = {
        title: {
          text: '预测值分布',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            const data = params[0]
            return `${data.name}<br/>样本数: ${data.value}`
          }
        },
        xAxis: {
          type: 'category',
          data: binLabels,
          axisLabel: { rotate: 45, fontSize: 10 }
        },
        yAxis: {
          type: 'value',
          name: '样本数'
        },
        series: [{
          name: '样本数',
          type: 'bar',
          data: bins,
          itemStyle: {
            color: '#409EFF'
          }
        }],
        grid: {
          left: '10%',
          right: '10%',
          bottom: '20%',
          top: '20%'
        }
      }

      chart.setOption(option)

      // 响应式调整
      window.addEventListener('resize', () => {
        chart.resize()
      })
    },

    /** 渲染置信度分布图 */
    renderConfidenceDistributionChart() {
      const chartElement = document.getElementById('confidenceDistributionChart')
      if (!chartElement || !this.detailedAnalysis || !this.detailedAnalysis.confidenceAnalysis) return

      const chart = echarts.init(chartElement)

      const confidenceData = this.detailedAnalysis.confidenceAnalysis

      const option = {
        title: {
          text: '置信度分布',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          textStyle: { fontSize: 12 }
        },
        series: [{
          name: '置信度分布',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['60%', '50%'],
          avoidLabelOverlap: false,
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '16',
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            {
              value: confidenceData.high.count,
              name: `高置信度 (≥0.8)`,
              itemStyle: { color: '#67C23A' }
            },
            {
              value: confidenceData.medium.count,
              name: `中等置信度 (0.6-0.8)`,
              itemStyle: { color: '#E6A23C' }
            },
            {
              value: confidenceData.low.count,
              name: `低置信度 (<0.6)`,
              itemStyle: { color: '#F56C6C' }
            }
          ]
        }]
      }

      chart.setOption(option)

      // 响应式调整
      window.addEventListener('resize', () => {
        chart.resize()
      })
    },

    /** 渲染深度-预测值关系图 */
    renderDepthPredictionChart() {
      const chartElement = document.getElementById('depthPredictionChart')
      if (!chartElement || !this.predictionResult || !this.predictionResult.previewData) return

      const chart = echarts.init(chartElement)

      // 获取数据
      const data = this.predictionResult.previewData
      const scatterData = data.map(row => {
        const depth = parseFloat(row['DEPTH']) || 0
        const prediction = parseFloat(row['预测值']) || 0
        const confidence = parseFloat(row['置信度']) || 0
        return [depth, prediction, confidence]
      }).filter(item => !isNaN(item[0]) && !isNaN(item[1]))

      if (scatterData.length === 0) {
        chartElement.innerHTML = '<div style="text-align: center; padding: 50px; color: #999;">暂无数据</div>'
        return
      }

      const option = {
        title: {
          text: '深度-预测值关系',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            const data = params.data
            return `深度: ${data[0].toFixed(2)}m<br/>预测值: ${data[1].toFixed(2)}<br/>置信度: ${data[2].toFixed(3)}`
          }
        },
        xAxis: {
          type: 'value',
          name: '深度 (m)',
          nameLocation: 'middle',
          nameGap: 30
        },
        yAxis: {
          type: 'value',
          name: '预测值',
          nameLocation: 'middle',
          nameGap: 40
        },
        series: [{
          name: '深度-预测值',
          type: 'scatter',
          data: scatterData,
          symbolSize: function(data) {
            // 根据置信度调整点的大小
            return Math.max(4, data[2] * 10)
          },
          itemStyle: {
            color: function(params) {
              // 根据置信度调整颜色
              const confidence = params.data[2]
              if (confidence >= 0.8) return '#67C23A'
              if (confidence >= 0.6) return '#E6A23C'
              return '#F56C6C'
            },
            opacity: 0.7
          }
        }],
        grid: {
          left: '15%',
          right: '10%',
          bottom: '15%',
          top: '20%'
        }
      }

      chart.setOption(option)

      // 响应式调整
      window.addEventListener('resize', () => {
        chart.resize()
      })
    },

    /** 渲染置信度-预测值关系图 */
    renderConfidencePredictionChart() {
      const chartElement = document.getElementById('confidencePredictionChart')
      if (!chartElement || !this.predictionResult || !this.predictionResult.previewData) return

      const chart = echarts.init(chartElement)

      // 获取数据
      const data = this.predictionResult.previewData
      const scatterData = data.map(row => {
        const confidence = parseFloat(row['置信度']) || 0
        const prediction = parseFloat(row['预测值']) || 0
        const depth = parseFloat(row['DEPTH']) || 0
        return [confidence, prediction, depth]
      }).filter(item => !isNaN(item[0]) && !isNaN(item[1]))

      if (scatterData.length === 0) {
        chartElement.innerHTML = '<div style="text-align: center; padding: 50px; color: #999;">暂无数据</div>'
        return
      }

      const option = {
        title: {
          text: '置信度-预测值关系',
          left: 'center',
          textStyle: { fontSize: 14 }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            const data = params.data
            return `置信度: ${data[0].toFixed(3)}<br/>预测值: ${data[1].toFixed(2)}<br/>深度: ${data[2].toFixed(2)}m`
          }
        },
        xAxis: {
          type: 'value',
          name: '置信度',
          nameLocation: 'middle',
          nameGap: 30,
          min: 0,
          max: 1
        },
        yAxis: {
          type: 'value',
          name: '预测值',
          nameLocation: 'middle',
          nameGap: 40
        },
        series: [{
          name: '置信度-预测值',
          type: 'scatter',
          data: scatterData,
          symbolSize: 6,
          itemStyle: {
            color: function(params) {
              // 根据置信度调整颜色
              const confidence = params.data[0]
              if (confidence >= 0.8) return '#67C23A'
              if (confidence >= 0.6) return '#E6A23C'
              return '#F56C6C'
            },
            opacity: 0.7
          }
        }],
        grid: {
          left: '15%',
          right: '10%',
          bottom: '15%',
          top: '20%'
        },
        // 添加置信度区间线
        markLine: {
          data: [
            { xAxis: 0.6, lineStyle: { color: '#E6A23C', type: 'dashed' } },
            { xAxis: 0.8, lineStyle: { color: '#67C23A', type: 'dashed' } }
          ],
          label: {
            show: true,
            position: 'end'
          }
        }
      }

      chart.setOption(option)

      // 响应式调整
      window.addEventListener('resize', () => {
        chart.resize()
      })
    },

    /** 获取预测变异性水平 */
    getPredictionVariabilityLevel() {
      const cv = this.calculateCoefficientOfVariation()
      if (cv < 10) return '低变异'
      if (cv < 30) return '中等变异'
      return '高变异'
    },

    /** 计算变异系数 */
    calculateCoefficientOfVariation() {
      if (!this.detailedAnalysis) return 0
      const stats = this.detailedAnalysis.basicStats.prediction
      return ((stats.std / stats.mean) * 100).toFixed(2)
    },

    /** 获取质量分数 */
    getQualityScore() {
      if (!this.detailedAnalysis) return 0
      const avgConfidence = parseFloat(this.detailedAnalysis.qualityAssessment.avgConfidence)
      return Math.round(avgConfidence * 100)
    },

    /** 获取质量颜色 */
    getQualityColor() {
      const score = this.getQualityScore()
      if (score >= 80) return '#67C23A'
      if (score >= 60) return '#E6A23C'
      return '#F56C6C'
    },

    /** 获取数据一致性 */
    getDataConsistency() {
      const cv = this.calculateCoefficientOfVariation()
      if (cv < 10) return '高'
      if (cv < 30) return '中'
      return '低'
    },

    /** 获取分层平均预测值 */
    getLayerAveragePrediction(layer) {
      if (!this.detailedAnalysis || !this.detailedAnalysis.depthAnalysis[layer]) {
        return 'N/A'
      }
      return this.detailedAnalysis.depthAnalysis[layer].avgPrediction || 'N/A'
    },

    /** 获取分层解释 */
    getLayerInterpretation(layer) {
      const interpretations = {
        shallow: '浅层通常含有较多的有机质，GR值相对较高',
        medium: '中层为过渡带，GR值变化较大',
        deep: '深层压实程度高，GR值相对稳定'
      }
      return interpretations[layer] || '暂无解释'
    },

    /** 获取岩性预测 */
    getLithologyPrediction() {
      if (!this.detailedAnalysis) return '暂无数据'
      const avgGR = this.detailedAnalysis.basicStats.prediction.mean
      if (avgGR < 50) return '主要为砂岩，储层条件较好'
      if (avgGR < 100) return '砂泥岩互层，储层条件中等'
      return '主要为泥岩，储层条件较差'
    },

    /** 计算四分位距 */
    calculateIQR() {
      if (!this.detailedAnalysis || !this.predictionResult || !this.predictionResult.previewData) return 'N/A'

      const data = this.predictionResult.previewData
      const predictions = data.map(row => parseFloat(row['预测值']) || 0).filter(v => !isNaN(v))

      if (predictions.length === 0) return 'N/A'

      const sorted = [...predictions].sort((a, b) => a - b)
      const q1 = sorted[Math.floor(sorted.length * 0.25)]
      const q3 = sorted[Math.floor(sorted.length * 0.75)]
      const iqr = q3 - q1

      return iqr.toFixed(2)
    },

    /** 计算偏度 */
    calculateSkewness() {
      if (!this.detailedAnalysis || !this.predictionResult || !this.predictionResult.previewData) return 'N/A'

      const data = this.predictionResult.previewData
      const predictions = data.map(row => parseFloat(row['预测值']) || 0).filter(v => !isNaN(v))

      if (predictions.length < 3) return 'N/A'

      const mean = predictions.reduce((a, b) => a + b, 0) / predictions.length
      const std = Math.sqrt(predictions.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / predictions.length)

      if (std === 0) return '0 (无变异)'

      const skewness = predictions.reduce((sum, val) => sum + Math.pow((val - mean) / std, 3), 0) / predictions.length

      let description = ''
      if (skewness > 0.5) description = '(右偏)'
      else if (skewness < -0.5) description = '(左偏)'
      else description = '(接近对称)'

      return `${skewness.toFixed(3)} ${description}`
    },

    /** 计算峰度 */
    calculateKurtosis() {
      if (!this.detailedAnalysis || !this.predictionResult || !this.predictionResult.previewData) return 'N/A'

      const data = this.predictionResult.previewData
      const predictions = data.map(row => parseFloat(row['预测值']) || 0).filter(v => !isNaN(v))

      if (predictions.length < 4) return 'N/A'

      const mean = predictions.reduce((a, b) => a + b, 0) / predictions.length
      const std = Math.sqrt(predictions.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / predictions.length)

      if (std === 0) return '0 (无变异)'

      const kurtosis = predictions.reduce((sum, val) => sum + Math.pow((val - mean) / std, 4), 0) / predictions.length - 3

      let description = ''
      if (kurtosis > 0) description = '(尖峰分布)'
      else if (kurtosis < 0) description = '(平峰分布)'
      else description = '(接近正态分布)'

      return `${kurtosis.toFixed(3)} ${description}`
    },

    /** 计算置信度标准差 */
    calculateConfidenceStd() {
      if (!this.detailedAnalysis || !this.predictionResult || !this.predictionResult.previewData) return 'N/A'

      const data = this.predictionResult.previewData
      const confidences = data.map(row => parseFloat(row['置信度']) || 0).filter(v => !isNaN(v))

      if (confidences.length === 0) return 'N/A'

      const mean = confidences.reduce((a, b) => a + b, 0) / confidences.length
      const variance = confidences.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / confidences.length
      const std = Math.sqrt(variance)

      return std.toFixed(3)
    },

    /** 获取预测稳定性 */
    getPredictionStability() {
      const cv = this.calculateCoefficientOfVariation()
      if (cv < 10) return '高稳定性'
      if (cv < 30) return '中等稳定性'
      return '低稳定性'
    },

    /** 获取模型优化建议 */
    getModelOptimizationRecommendations() {
      const recommendations = []
      const avgConfidence = parseFloat(this.detailedAnalysis.qualityAssessment.avgConfidence)

      if (avgConfidence < 0.7) {
        recommendations.push('考虑增加训练数据量以提高模型准确性')
        recommendations.push('尝试调整模型超参数，如学习率、正则化参数等')
      }

      if (this.detailedAnalysis.confidenceAnalysis.low.percentage > 20) {
        recommendations.push('对低置信度样本进行特征工程优化')
        recommendations.push('考虑使用集成学习方法提高预测稳定性')
      }

      recommendations.push('定期使用新数据重新训练模型')
      recommendations.push('考虑添加更多相关特征变量')

      return recommendations
    },

    /** 获取数据改进建议 */
    getDataImprovementRecommendations() {
      const recommendations = []
      const depthRange = this.detailedAnalysis.basicStats.depth.range

      if (depthRange < 100) {
        recommendations.push('扩大深度采样范围以提高模型泛化能力')
      }

      recommendations.push('增加数据质量控制，去除异常值和噪声')
      recommendations.push('考虑添加更多地质参数作为输入特征')
      recommendations.push('确保数据在不同深度层的均匀分布')

      return recommendations
    },

    /** 获取应用建议 */
    getApplicationRecommendations() {
      const recommendations = []
      const avgConfidence = parseFloat(this.detailedAnalysis.qualityAssessment.avgConfidence)

      if (avgConfidence >= 0.8) {
        recommendations.push('预测结果可直接用于地质解释和决策')
      } else if (avgConfidence >= 0.6) {
        recommendations.push('预测结果可作为参考，建议结合其他地质资料')
      } else {
        recommendations.push('预测结果仅供参考，需要更多验证')
      }

      recommendations.push('建议对低置信度区域进行重点关注和验证')
      recommendations.push('结合测井曲线和地质背景进行综合分析')

      return recommendations
    },

    /** 获取警告信息 */
    getWarnings() {
      const warnings = []
      const lowConfidenceCount = this.detailedAnalysis.confidenceAnalysis.low.count

      if (lowConfidenceCount > 0) {
        warnings.push(`存在${lowConfidenceCount}个低置信度预测，需要谨慎使用`)
      }

      if (this.detailedAnalysis.predictionDistribution.outliers.length > 0) {
        warnings.push('检测到异常预测值，可能存在数据质量问题')
      }

      warnings.push('预测结果仅基于历史数据，实际应用时需考虑地质条件变化')
      warnings.push('建议定期更新模型以保持预测准确性')

      return warnings
    },

    /** 打印分析报告 */
    printAnalysisReport() {
      window.print()
    },

    /** 监听标签页切换 */
    handleTabChange(tab) {
      if (tab.name === 'charts') {
        this.$nextTick(() => {
          this.renderAnalysisCharts()
        })
      }
    },



    parseTime
  }
}
</script>

<style scoped>
.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}
.clearfix:after {
  clear: both
}

/* 详细分析样式 */
.confidence-item {
  padding: 15px;
  border-radius: 6px;
  text-align: center;
}

.high-confidence {
  background-color: #f0f9ff;
  border: 1px solid #67C23A;
}

.medium-confidence {
  background-color: #fdf6ec;
  border: 1px solid #E6A23C;
}

.low-confidence {
  background-color: #fef0f0;
  border: 1px solid #F56C6C;
}

.quality-assessment {
  text-align: center;
  padding: 20px;
}

.recommendations-list {
  list-style: none;
  padding: 0;
}

.recommendations-list li {
  padding: 8px 0;
  border-bottom: 1px solid #EBEEF5;
}

.recommendations-list li:last-child {
  border-bottom: none;
}

/* 详细分析对话框样式 */
.detailed-analysis-dialog .el-dialog__body {
  padding: 10px 20px;
}

/* 概览卡片样式 */
.analysis-overview {
  margin-bottom: 20px;
}

.overview-card {
  border-radius: 8px;
  transition: all 0.3s;
}

.overview-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.overview-item {
  display: flex;
  align-items: center;
  padding: 20px;
}

.overview-icon {
  font-size: 32px;
  margin-right: 15px;
}

.overview-content {
  flex: 1;
}

.overview-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.overview-label {
  font-size: 14px;
  color: #909399;
}

/* 洞察样式 */
.insight-item {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 15px;
}

.insight-item h4 {
  margin: 0 0 10px 0;
  color: #409EFF;
  font-size: 16px;
}

.insight-item p {
  margin: 5px 0;
  color: #606266;
}

/* 统计项样式 */
.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #EBEEF5;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  color: #606266;
  font-weight: 500;
}

.stat-value {
  color: #303133;
  font-weight: bold;
}

/* 置信度分布条 */
.confidence-distribution {
  margin-top: 15px;
}

.confidence-bar {
  display: flex;
  height: 30px;
  border-radius: 15px;
  overflow: hidden;
  margin-bottom: 10px;
}

.confidence-segment {
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 12px;
  min-width: 30px;
}

.confidence-segment.high {
  background-color: #67C23A;
}

.confidence-segment.medium {
  background-color: #E6A23C;
}

.confidence-segment.low {
  background-color: #F56C6C;
}

.confidence-legend {
  display: flex;
  justify-content: space-around;
  font-size: 12px;
}

.legend-item {
  padding: 2px 8px;
  border-radius: 10px;
  color: white;
}

.legend-item.high {
  background-color: #67C23A;
}

.legend-item.medium {
  background-color: #E6A23C;
}

.legend-item.low {
  background-color: #F56C6C;
}

/* 深度分布样式 */
.depth-distribution {
  margin-top: 15px;
}

.depth-segment {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #EBEEF5;
}

.depth-segment:last-child {
  border-bottom: none;
}

.depth-label {
  font-weight: bold;
  color: #409EFF;
}

.depth-range {
  color: #606266;
  font-size: 12px;
}

.depth-count {
  color: #67C23A;
  font-weight: bold;
}

/* 质量评估样式 */
.quality-assessment-detailed {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.quality-score {
  margin-right: 30px;
}

.quality-score-text {
  font-size: 18px;
  font-weight: bold;
  display: block;
}

.quality-score-label {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 5px;
}

.quality-details {
  flex: 1;
}

.quality-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.quality-metric {
  color: #606266;
  font-weight: 500;
}

.quality-value {
  color: #303133;
  font-weight: bold;
}

.quality-description {
  padding: 15px;
  background: #f0f9ff;
  border-radius: 6px;
  border-left: 4px solid #409EFF;
}

/* 异常检测样式 */
.anomaly-detection {
  padding: 15px;
}

.anomaly-item {
  margin-bottom: 20px;
}

.anomaly-item h4 {
  margin: 0 0 10px 0;
  color: #E6A23C;
  font-size: 16px;
}

.anomaly-item p {
  margin: 5px 0;
  color: #606266;
}

/* 地质解释样式 */
.geological-interpretation {
  padding: 15px;
}

.interpretation-item {
  margin-bottom: 25px;
}

.interpretation-item h4 {
  margin: 0 0 15px 0;
  color: #409EFF;
  font-size: 16px;
}

.depth-layers {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 15px;
}

.layer-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 15px;
  padding: 10px;
  background: white;
  border-radius: 4px;
  border-left: 4px solid #409EFF;
}

.layer-item:last-child {
  margin-bottom: 0;
}

.layer-name {
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.layer-prediction {
  color: #67C23A;
  font-weight: bold;
  margin-bottom: 5px;
}

.layer-interpretation {
  color: #606266;
  font-size: 14px;
}

/* 统计指标样式 */
.statistical-metrics {
  padding: 15px;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #EBEEF5;
}

.metric-item:last-child {
  border-bottom: none;
}

.metric-name {
  color: #606266;
  font-weight: 500;
}

.metric-value {
  color: #303133;
  font-weight: bold;
}

/* 建议样式 */
.recommendations-section {
  padding: 15px;
}

.recommendation-category {
  margin-bottom: 25px;
}

.recommendation-category h4 {
  margin: 0 0 15px 0;
  color: #409EFF;
  font-size: 16px;
}

.recommendation-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.recommendation-list li {
  padding: 8px 0;
  border-bottom: 1px solid #EBEEF5;
  color: #606266;
}

.recommendation-list li:last-child {
  border-bottom: none;
}

.recommendation-list.warning li {
  color: #E6A23C;
}

.recommendation-list li i {
  margin-right: 8px;
  color: #67C23A;
}

.recommendation-list.warning li i {
  color: #E6A23C;
}
</style>
