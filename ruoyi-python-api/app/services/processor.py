# app/services/processor.py
import importlib
import json
import logging
from pathlib import Path
import sys

from app.schemas.task import TaskSubmitRequest

# 确保 algorithms 目录可以被找到
# 在 FastAPI 的上下文中，通常不需要手动添加 sys.path，因为 uvicorn 会处理好根路径
# 但为了保险起见，我们进行确认
project_root = Path(__file__).resolve().parent.parent
sys.path.append(str(project_root))

logging.basicConfig(level=logging.INFO, format='[%(levelname)s][%(module)s] %(message)s')

def dynamic_import(module_name, class_name):
    """动态地从给定的模块字符串中导入一个类。"""
    try:
        logging.debug(f"尝试从模块 '{module_name}' 导入类 '{class_name}'")
        module = importlib.import_module(module_name)
        return getattr(module, class_name)
    except (ImportError, AttributeError) as e:
        logging.error(f"从 {module_name} 导入 {class_name} 失败: {e}", exc_info=True)
        raise RuntimeError(f"无法加载指定的算法组件: {module_name}.{class_name}")

def run_task(task: TaskSubmitRequest):
    """
    执行分析/预测任务的核心逻辑。
    这段代码是从原来的 predict_processor.py 中重构而来的。
    """
    logging.info(f"开始执行任务 '{task.task_name}'，使用算法 '{task.algorithm}'...")
    
    try:
        # --- 新的、更健壮的解析逻辑 ---
        algorithm_str = task.algorithm
        parts = algorithm_str.split('_')

        if len(parts) < 2:
            raise ValueError(f"算法名称 '{algorithm_str}' 格式不正确，应为 'category_name_action' (例如 'regression_lightgbm_train')")

        action = parts[-1]

        if action == "train":
            class_to_load = "Trainer"
        elif action == "predict":
            class_to_load = "Predictor"
        else:
            raise ValueError(f"无法从算法名称 '{algorithm_str}' 中解析出有效的操作 (train/predict)")

        # 特殊处理特征工程算法
        if algorithm_str.startswith("feature_engineering_"):
            # 例如 'feature_engineering_fractal_dimension_train' -> 'algorithms.feature_engineering.fractal_dimension'
            name_parts = parts[2:-1]  # 跳过 'feature_engineering' 和 'train'
            name = "_".join(name_parts)
            full_module_path = f"algorithms.feature_engineering.{name}"
        else:
            # 常规算法处理
            # 例如 'regression_lightgbm_train' -> 'algorithms.regression.lightgbm'
            category = parts[0]
            name = "_".join(parts[1:-1])
            full_module_path = f"algorithms.{category}.{name}"
        
        AlgorithmClass = dynamic_import(full_module_path, class_to_load)
        # --- 逻辑修改结束 ---

        logging.debug(f"动态加载算法类: {AlgorithmClass} from {full_module_path}")

        # 实例化并运行算法
        # BaseAlgorithm 期望三个参数: file_path, output_dir, params
        algorithm_instance = AlgorithmClass(
            file_path=task.input_file_path,
            output_dir=task.output_dir_path,
            params=task.input_params
        )
        result = algorithm_instance.run()
        
        logging.info(f"任务 '{task.task_name}' 成功完成。结果: {result}")
        return result

    except Exception as e:
        logging.error(f"执行任务 '{task.task_name}' 时发生异常。", exc_info=True)
        # 在真实应用中，这里会更新 Celery 任务的状态为 FAILED
        # 现在我们只是重新抛出异常，让 API 端点捕获
        raise e 