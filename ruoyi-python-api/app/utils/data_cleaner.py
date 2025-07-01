# app/utils/data_cleaner.py
import pandas as pd
import numpy as np
from typing import List, Tuple, Union

def clean_regression_data(
    dataframe: pd.DataFrame, 
    feature_cols: List[str], 
    target_col: str,
    remove_outliers: bool = True,
    outlier_percentile: float = 99.9
) -> Tuple[pd.DataFrame, pd.DataFrame, pd.Series]:
    """
    清理回归数据，处理NaN、无穷大值和异常值
    
    Args:
        dataframe: 原始数据框
        feature_cols: 特征列名列表
        target_col: 目标列名
        remove_outliers: 是否移除异常值
        outlier_percentile: 异常值截断的百分位数
    
    Returns:
        Tuple[清理后的完整数据框, 特征数据框, 目标序列]
    
    Raises:
        ValueError: 当数据清理后没有有效数据时
    """
    
    # 1. 选择需要的列
    required_cols = feature_cols + [target_col]
    missing_cols = [col for col in required_cols if col not in dataframe.columns]
    if missing_cols:
        raise ValueError(f"数据中缺少以下列: {missing_cols}")
    
    # 2. 创建工作副本
    work_df = dataframe[required_cols].copy()
    
    # 3. 处理无穷大值 - 替换为NaN
    work_df = work_df.replace([np.inf, -np.inf], np.nan)
    
    # 4. 记录清理前的数据量
    original_count = len(work_df)
    
    # 5. 删除包含NaN的行
    work_df = work_df.dropna()
    
    if len(work_df) == 0:
        raise ValueError("数据清理后没有有效数据，所有行都包含NaN或无穷大值")
    
    # 6. 处理异常值（如果启用）
    if remove_outliers and len(work_df) > 10:  # 至少需要10行数据才进行异常值处理
        # 对每个特征列进行异常值截断
        for col in feature_cols:
            if col in work_df.columns:
                col_data = work_df[col]
                if len(col_data) > 0 and col_data.std() > 0:  # 确保有数据且有变化
                    lower_bound = np.percentile(col_data, 100 - outlier_percentile)
                    upper_bound = np.percentile(col_data, outlier_percentile)
                    work_df[col] = np.clip(col_data, lower_bound, upper_bound)
        
        # 对目标列进行异常值截断
        if target_col in work_df.columns:
            target_data = work_df[target_col]
            if len(target_data) > 0 and target_data.std() > 0:
                lower_bound = np.percentile(target_data, 100 - outlier_percentile)
                upper_bound = np.percentile(target_data, outlier_percentile)
                work_df[target_col] = np.clip(target_data, lower_bound, upper_bound)
    
    # 7. 最终数据验证
    if np.any(work_df.isna()):
        raise ValueError("数据清理后仍包含NaN值")
    
    if np.any(np.isinf(work_df.values)):
        raise ValueError("数据清理后仍包含无穷大值")
    
    # 8. 检查数据变化
    if len(work_df) < original_count * 0.1:  # 如果清理后数据少于原来的10%
        print(f"警告: 数据清理移除了 {original_count - len(work_df)} 行数据 "
              f"({(original_count - len(work_df)) / original_count * 100:.1f}%)")
    
    # 9. 分离特征和目标
    X_df = work_df[feature_cols]
    y_series = work_df[target_col]
    
    return work_df, X_df, y_series


def clean_prediction_data(
    dataframe: pd.DataFrame,
    feature_cols: List[str],
    remove_outliers: bool = True,
    outlier_percentile: float = 99.9
) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    清理预测数据，只处理特征列（目标列为空或不存在）

    Args:
        dataframe: 原始数据框
        feature_cols: 特征列名列表
        remove_outliers: 是否移除异常值
        outlier_percentile: 异常值截断的百分位数

    Returns:
        Tuple[清理后的完整数据框, 特征数据框]

    Raises:
        ValueError: 当数据清理后没有有效数据时
    """

    # 1. 检查特征列是否存在
    missing_cols = [col for col in feature_cols if col not in dataframe.columns]
    if missing_cols:
        raise ValueError(f"数据中缺少以下特征列: {missing_cols}")

    # 2. 创建工作副本（只包含特征列）
    work_df = dataframe[feature_cols].copy()

    # 3. 处理无穷大值 - 替换为NaN
    work_df = work_df.replace([np.inf, -np.inf], np.nan)

    # 4. 记录清理前的数据量
    original_count = len(work_df)

    # 5. 处理特征列的NaN值 - 使用均值填充而不是删除行
    for col in feature_cols:
        if work_df[col].isnull().any():
            missing_count = work_df[col].isnull().sum()
            print(f"特征列 '{col}' 包含 {missing_count} 个缺失值，将使用均值填充")

            # 计算均值（排除NaN）
            col_mean = work_df[col].mean()
            if pd.isna(col_mean):
                # 如果所有值都是NaN，使用0填充
                print(f"警告: 特征列 '{col}' 所有值都是缺失值，使用0填充")
                work_df[col] = work_df[col].fillna(0)
            else:
                work_df[col] = work_df[col].fillna(col_mean)

    # 6. 处理异常值（如果启用）
    if remove_outliers and len(work_df) > 10:
        for col in feature_cols:
            if col in work_df.columns:
                col_data = work_df[col]
                if len(col_data) > 0 and col_data.std() > 0:
                    lower_bound = np.percentile(col_data, 100 - outlier_percentile)
                    upper_bound = np.percentile(col_data, outlier_percentile)
                    work_df[col] = np.clip(col_data, lower_bound, upper_bound)

    # 7. 最终数据验证
    if np.any(work_df.isna()):
        raise ValueError("预测数据清理后仍包含NaN值")

    if np.any(np.isinf(work_df.values)):
        raise ValueError("预测数据清理后仍包含无穷大值")

    # 8. 检查数据量
    if len(work_df) == 0:
        raise ValueError("预测数据清理后没有有效数据")

    print(f"预测数据清理完成: {len(work_df)} 行数据, {len(feature_cols)} 个特征")

    return work_df, work_df[feature_cols]


def validate_prediction_data(X: Union[np.ndarray, pd.DataFrame]) -> None:
    """
    验证预测数据的质量（只验证特征数据，不需要目标列）

    Args:
        X: 特征数据

    Raises:
        ValueError: 当数据不符合机器学习要求时
    """

    # 转换为numpy数组进行验证
    if hasattr(X, 'values'):
        X_array = X.values
    else:
        X_array = np.array(X)

    # 检查数据形状
    if X_array.ndim != 2:
        raise ValueError(f"特征数据必须是2维的，当前维度: {X_array.ndim}")

    # 检查数据量
    if X_array.shape[0] < 1:
        raise ValueError("预测数据至少需要1行数据")

    if X_array.shape[1] < 1:
        raise ValueError("预测数据至少需要1个特征")

    # 检查NaN值
    if np.any(np.isnan(X_array)):
        raise ValueError("预测数据包含NaN值")

    # 检查无穷大值
    if np.any(np.isinf(X_array)):
        raise ValueError("预测数据包含无穷大值")

    print(f"✅ 预测数据验证通过: {X_array.shape[0]} 行, {X_array.shape[1]} 个特征")


def validate_data_for_ml(X: Union[np.ndarray, pd.DataFrame], y: Union[np.ndarray, pd.Series]) -> None:
    """
    验证机器学习数据的质量
    
    Args:
        X: 特征数据
        y: 目标数据
    
    Raises:
        ValueError: 当数据不符合机器学习要求时
    """
    
    # 转换为numpy数组进行检查
    if isinstance(X, pd.DataFrame):
        X_array = X.values
    else:
        X_array = X
        
    if isinstance(y, pd.Series):
        y_array = y.values
    else:
        y_array = y
    
    # 检查形状匹配
    if len(X_array) != len(y_array):
        raise ValueError(f"特征数据长度 ({len(X_array)}) 与目标数据长度 ({len(y_array)}) 不匹配")
    
    # 检查是否有足够的数据
    if len(X_array) < 2:
        raise ValueError("数据量不足，至少需要2行数据")
    
    # 检查NaN
    if np.any(np.isnan(X_array)):
        raise ValueError("特征数据包含NaN值")
    if np.any(np.isnan(y_array)):
        raise ValueError("目标数据包含NaN值")
    
    # 检查无穷大
    if np.any(np.isinf(X_array)):
        raise ValueError("特征数据包含无穷大值")
    if np.any(np.isinf(y_array)):
        raise ValueError("目标数据包含无穷大值")
    
    # 检查目标变量的变化
    if len(np.unique(y_array)) == 1:
        raise ValueError("目标变量没有变化，所有值都相同")
    
    print(f"数据验证通过: {len(X_array)} 行数据, {X_array.shape[1]} 个特征")


def get_data_quality_report(dataframe: pd.DataFrame, feature_cols: List[str], target_col: str) -> dict:
    """
    生成数据质量报告
    
    Args:
        dataframe: 数据框
        feature_cols: 特征列名列表
        target_col: 目标列名
    
    Returns:
        数据质量报告字典
    """
    
    report = {
        "total_rows": len(dataframe),
        "feature_columns": len(feature_cols),
        "missing_data": {},
        "infinite_data": {},
        "data_types": {},
        "basic_stats": {}
    }
    
    # 检查每列的数据质量
    for col in feature_cols + [target_col]:
        if col in dataframe.columns:
            col_data = dataframe[col]
            
            # 缺失值
            missing_count = col_data.isna().sum()
            report["missing_data"][col] = {
                "count": int(missing_count),
                "percentage": float(missing_count / len(dataframe) * 100)
            }
            
            # 无穷大值
            inf_count = np.isinf(col_data.replace([np.inf, -np.inf], np.nan).fillna(0)).sum()
            report["infinite_data"][col] = {
                "count": int(inf_count),
                "percentage": float(inf_count / len(dataframe) * 100)
            }
            
            # 数据类型
            report["data_types"][col] = str(col_data.dtype)
            
            # 基本统计
            if pd.api.types.is_numeric_dtype(col_data):
                valid_data = col_data.dropna()
                if len(valid_data) > 0:
                    report["basic_stats"][col] = {
                        "mean": float(valid_data.mean()),
                        "std": float(valid_data.std()),
                        "min": float(valid_data.min()),
                        "max": float(valid_data.max()),
                        "unique_values": int(valid_data.nunique())
                    }
    
    return report
