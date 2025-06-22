package com.ruoyi.common.enums;

import lombok.Getter;

@Getter
public enum AlgorithmType {
    FRACTAL_DIMENSION("fractal_dimension", "分形维数"),
    LINEAR_REGRESSION("linear_regression", "线性回归"),
    POLYNOMIAL_REGRESSION("polynomial_regression", "多项式回归"),
    EXPONENTIAL_REGRESSION("exponential_regression", "指数回归"),
    LOGARITHMIC_REGRESSION("logarithmic_regression", "对数回归"),
    AUTOMATIC_REGRESSION("automatic_regression", "自动回归"),
    SVM_REGRESSION("svm_regression", "支持向量机回归"),
    RANDOM_FOREST_REGRESSION("random_forest_regression", "随机森林回归"),
    XGBOOST_REGRESSION("xgboost_regression", "XGBoost回归"),
    LIGHTGBM_REGRESSION("lightgbm_regression", "LightGBM回归"),
    BILSTM_REGRESSION("bilstm_regression", "BiLSTM回归"),
    TCN_REGRESSION("tcn_regression", "TCN回归"),

    LINEAR_REGRESSION_PREDICT("linear_regression_predict", "线性回归预测"),
    POLYNOMIAL_REGRESSION_PREDICT("polynomial_regression_predict", "多项式回归预测"),
    SVM_REGRESSION_PREDICT("svm_regression_predict", "支持向量机预测"),
    RANDOM_FOREST_REGRESSION_PREDICT("random_forest_regression_predict", "随机森林预测"),
    XGBOOST_REGRESSION_PREDICT("xgboost_regression_predict", "XGBoost预测"),
    LIGHTGBM_REGRESSION_PREDICT("lightgbm_regression_predict", "LightGBM预测"),
    BILSTM_REGRESSION_PREDICT("bilstm_regression_predict", "BiLSTM预测"),
    TCN_REGRESSION_PREDICT("tcn_regression_predict", "TCN预测"),

    /**
     * 分类算法
     */
    LOGISTIC_REGRESSION("logistic_regression", "逻辑回归分类"),
    SVM_CLASSIFICATION("svm_classification", "SVM分类"),
    KNN_CLASSIFICATION("knn_classification", "KNN分类"),

    /**
     * 聚类算法
     */
    KMEANS_CLUSTERING("kmeans_clustering", "KMeans聚类");


    private final String value;
    private final String description;

    AlgorithmType(String value, String description) {
        this.value = value;
        this.description = description;
    }
} 