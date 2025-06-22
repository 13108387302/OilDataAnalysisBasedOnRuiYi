import logging
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
from sklearn.metrics import mean_squared_error, r2_score, mean_absolute_error

def calculate_regression_metrics(y_true, y_pred):
    """计算回归分析的核心指标"""
    metrics = {
        "MSE": mean_squared_error(y_true, y_pred),
        "R2": r2_score(y_true, y_pred),
        "MAE": mean_absolute_error(y_true, y_pred),
        "RMSE": np.sqrt(mean_squared_error(y_true, y_pred))
    }
    # 转换为原生 float 类型以便 JSON 序列化
    return {k: float(v) for k, v in metrics.items()}

def plot_regression_results(output_dir: Path, y_true: np.ndarray, y_pred: np.ndarray, title: str):
    """
    绘制回归结果的真实值 vs 预测值散点图，并将其保存到指定目录。
    
    Args:
        output_dir (Path): 保存图像的目标目录.
        y_true (np.ndarray): 真实值.
        y_pred (np.ndarray): 预测值.
        title (str): 图表标题.
        
    Returns:
        str: 保存的图像文件的相对Web路径 (相对于/profile/).
    """
    plt.style.use('seaborn-v0_8-whitegrid')
    fig, ax = plt.subplots(figsize=(10, 6))
    
    ax.scatter(y_true, y_pred, alpha=0.6, edgecolors='k', label='Predicted vs. True')
    
    # 添加 1:1 对角线
    lims = [
        np.min([ax.get_xlim(), ax.get_ylim()]),
        np.max([ax.get_xlim(), ax.get_ylim()]),
    ]
    ax.plot(lims, lims, 'r--', alpha=0.75, zorder=0, label='Ideal Fit (1:1 Line)')
    
    ax.set_xlabel("True Values", fontsize=12)
    ax.set_ylabel("Predicted Values", fontsize=12)
    ax.set_title(title, fontsize=14, weight='bold')
    ax.legend()
    ax.grid(True)
    
    # 确保输出目录存在
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # 保存图像
    image_name = f"{title.replace(' ', '_').lower()}_plot.png"
    save_path = output_dir / image_name
    
    try:
        plt.savefig(save_path, bbox_inches='tight', dpi=150)
        plt.close(fig) # 关闭图形，释放内存
        logging.info(f"结果图已保存至: {save_path}")

        # --- 重要: 返回相对于 /profile/ 的路径，以便前端可以访问 ---
        # 我们需要找到 "petrol" 在路径中的位置
        parts = save_path.parts
        try:
            # 在Windows上，路径可能是 E:\...，需要正确处理
            # 我们假设 'petrol' 是路径的一部分
            petrol_index = parts.index('petrol')
            web_path = Path(*parts[petrol_index:]).as_posix() # as_posix() 确保使用 /
            # 返回 /profile/ 开头的路径，不要重复 petrol
            return f"/profile/{web_path}"
        except ValueError:
            # 如果路径中没有 'petrol'，返回一个 fallback，但这表示配置可能出错了
            logging.warning(f"无法从路径 {save_path} 创建web可访问路径，将返回绝对路径。")
            return str(save_path)

    except Exception as e:
        logging.error(f"保存图像时出错: {e}", exc_info=True)
        plt.close(fig)
        return None 