"""
分析任务API端点
处理机器学习分析任务的创建和执行
注意：已移除所有模拟数据，只使用真实数据和真实模型
"""

from fastapi import APIRouter
from typing import List, Optional, Dict, Any
import logging
from app.schemas.task import PredictRequest, TaskSubmitRequest
from pydantic import BaseModel, Field

# 定义AnalysisRequest模型
class AnalysisRequest(BaseModel):
    """
    分析任务请求模型
    """
    task_name: str = Field(..., example="分析任务")
    task_type: str = Field(..., example="analysis")
    algorithm: str = Field(..., example="linear_regression")
    features: List[str] = Field(..., example=["DEPTH", "GR"])
    target: str = Field(..., example="POROSITY")
    data_file: Optional[Dict[str, Any]] = Field(None, example={"name": "data.csv"})
    parameters: Optional[Dict[str, Any]] = Field(None, example={})

# 错误码定义
class ErrorCodes:
    SUCCESS = 200
    DEPENDENCY_MISSING = 1001
    ALGORITHM_NOT_IMPLEMENTED = 1002
    ALGORITHM_EXECUTION_FAILED = 1003
    TASK_PROCESSING_FAILED = 1004
    PREDICTION_FAILED = 1005
    BATCH_PREDICTION_FAILED = 1006
    INVALID_PARAMETERS = 1007
    FILE_NOT_FOUND = 1008
    MODEL_NOT_FOUND = 1009
    DATA_VALIDATION_FAILED = 1010


def check_ml_dependencies():
    """检查机器学习依赖包，返回错误响应或None"""
    try:
        import sklearn
        import pandas as pd
        import numpy as np
        logging.info("✅ 检测到机器学习依赖包")
        return None
    except ImportError as e:
        missing_packages = []
        try:
            import sklearn  # noqa: F401
        except ImportError:
            missing_packages.append("scikit-learn")

        try:
            import pandas  # noqa: F401
        except ImportError:
            missing_packages.append("pandas")

        try:
            import numpy  # noqa: F401
        except ImportError:
            missing_packages.append("numpy")

        error_message = f"缺少必需的机器学习依赖包: {', '.join(missing_packages)}。请安装: pip install {' '.join(missing_packages)}"
        logging.error(f"❌ [错误码:{ErrorCodes.DEPENDENCY_MISSING}] {error_message}")

        return create_error_response(
            error_code=ErrorCodes.DEPENDENCY_MISSING,
            message=error_message,
            missing_packages=missing_packages,
            installation_command=f"pip install {' '.join(missing_packages)}"
        )

# 错误信息映射
ERROR_MESSAGES = {
    ErrorCodes.DEPENDENCY_MISSING: "缺少必需的依赖包",
    ErrorCodes.ALGORITHM_NOT_IMPLEMENTED: "算法未实现",
    ErrorCodes.ALGORITHM_EXECUTION_FAILED: "算法执行失败",
    ErrorCodes.TASK_PROCESSING_FAILED: "任务处理失败",
    ErrorCodes.PREDICTION_FAILED: "预测执行失败",
    ErrorCodes.BATCH_PREDICTION_FAILED: "批量预测执行失败",
    ErrorCodes.INVALID_PARAMETERS: "参数验证失败",
    ErrorCodes.FILE_NOT_FOUND: "文件不存在",
    ErrorCodes.MODEL_NOT_FOUND: "模型不存在",
    ErrorCodes.DATA_VALIDATION_FAILED: "数据验证失败"
}

def create_error_response(error_code: int, message: str = None, **kwargs):
    """创建标准错误响应"""
    error_message = message or ERROR_MESSAGES.get(error_code, "未知错误")
    return {
        "success": False,
        "error_code": error_code,
        "error": ERROR_MESSAGES.get(error_code, "未知错误"),
        "message": error_message,
        "status": "failed",
        **kwargs
    }

def create_success_response(data: Any = None, message: str = "操作成功", **kwargs):
    """创建标准成功响应"""
    return {
        "success": True,
        "error_code": ErrorCodes.SUCCESS,
        "message": message,
        "data": data,
        "status": "success",
        **kwargs
    }

def convert_profile_path(path: str) -> str:
    """
    转换/profile/路径为实际文件系统路径
    /profile/xxx -> 项目根目录/data/xxx
    """
    if not path:
        return path

    if path.startswith("/profile/"):
        import os
        relative_path = path[len("/profile/"):]

        # 获取项目根目录：从当前文件位置向上查找，直到找到包含data目录的目录
        current_file = os.path.abspath(__file__)
        current_dir = os.path.dirname(current_file)

        # 向上查找项目根目录
        project_root = current_dir
        for _ in range(5):  # 最多向上查找5级
            parent_dir = os.path.dirname(project_root)
            if parent_dir == project_root:  # 已经到达根目录
                break
            project_root = parent_dir

            # 检查是否找到了包含data目录的项目根目录
            data_dir = os.path.join(project_root, "data")
            if os.path.exists(data_dir):
                break

        converted_path = os.path.join(project_root, "data", relative_path).replace("\\", "/")

        logging.info(f"路径转换: {path} -> {converted_path}")
        logging.info(f"项目根目录: {project_root}")
        return converted_path

    return path

def extract_data_file_path(data_file: dict) -> str:
    """
    从前端传递的data_file对象中提取文件路径

    前端可能传递的字段：
    - path: 直接路径
    - serverPath: 服务器路径
    - actualPath: 实际路径

    Args:
        data_file: 前端传递的文件信息字典

    Returns:
        转换后的文件路径
    """
    if not data_file:
        return ""

    # 按优先级尝试不同的路径字段
    path_candidates = [
        data_file.get("path"),
        data_file.get("serverPath"),
        data_file.get("actualPath"),
        data_file.get("filePath")
    ]

    for path in path_candidates:
        if path:
            logging.info(f"🔍 提取到数据文件路径: {path}")
            return convert_profile_path(path)

    # 如果没有找到路径，记录详细信息
    logging.error(f"❌ 无法从data_file中提取路径: {data_file}")
    return ""

# 配置日志
logging.basicConfig(level=logging.INFO)

router = APIRouter()


@router.post("/submit")
async def submit_task(task_request: TaskSubmitRequest):
    """
    提交任务端点 - Java后端调用的主要接口
    调用已实现的机器学习算法
    """
    try:
        logging.info(f"收到任务提交请求: {task_request.model_dump()}")

        # 记录任务详细信息
        logging.info(f"任务ID: {task_request.id}")
        logging.info(f"任务名称: {task_request.task_name}")
        logging.info(f"算法: {task_request.algorithm}")
        logging.info(f"输入文件: {task_request.input_file_path}")
        logging.info(f"输出目录: {task_request.output_dir_path}")
        logging.info(f"参数: {task_request.input_params}")

        # 检查必需的依赖包
        try:
            import sklearn
            import pandas as pd
            import numpy as np
            logging.info("✅ 检测到机器学习依赖包")

        except ImportError as e:
            missing_packages = []
            try:
                import sklearn  # noqa: F401
            except ImportError:
                missing_packages.append("scikit-learn")

            try:
                import pandas  # noqa: F401
            except ImportError:
                missing_packages.append("pandas")

            try:
                import numpy  # noqa: F401
            except ImportError:
                missing_packages.append("numpy")

            error_message = f"缺少必需的机器学习依赖包: {', '.join(missing_packages)}。请安装: pip install {' '.join(missing_packages)}"
            logging.error(f"❌ [错误码:{ErrorCodes.DEPENDENCY_MISSING}] {error_message}")

            return create_error_response(
                error_code=ErrorCodes.DEPENDENCY_MISSING,
                message=error_message,
                task_id=task_request.id,
                task_name=task_request.task_name,
                algorithm=task_request.algorithm,
                missing_packages=missing_packages,
                installation_command=f"pip install {' '.join(missing_packages)}"
            )

        # 调用已实现的算法
        try:
            from app.services.processor import run_task

            logging.info(f"开始执行算法: {task_request.algorithm}")
            result = run_task(task_request)

            logging.info(f"✅ 算法执行成功，任务ID: {task_request.id}")
            return result

        except Exception as algorithm_error:
            error_message = f"算法执行失败: {str(algorithm_error)}"
            logging.error(f"❌ [错误码:{ErrorCodes.ALGORITHM_EXECUTION_FAILED}] {error_message}", exc_info=True)

            return create_error_response(
                error_code=ErrorCodes.ALGORITHM_EXECUTION_FAILED,
                message=error_message,
                task_id=task_request.id,
                task_name=task_request.task_name,
                algorithm=task_request.algorithm,
                algorithm_error=True,
                details=str(algorithm_error)
            )

    except Exception as e:
        error_message = f"任务提交处理失败: {str(e)}"
        logging.error(f"❌ [错误码:{ErrorCodes.TASK_PROCESSING_FAILED}] {error_message}", exc_info=True)

        return create_error_response(
            error_code=ErrorCodes.TASK_PROCESSING_FAILED,
            message=error_message,
            task_id=getattr(task_request, 'id', None),
            exception=True
        )



@router.post("/predict")
async def predict_data(predict_request: PredictRequest):
    """
    执行预测任务
    使用已训练的机器学习模型进行预测
    """
    try:
        logging.info(f"开始处理预测任务请求: {predict_request.model_dump()}")

        # 检查必需的依赖包
        dependency_error = check_ml_dependencies()
        if dependency_error:
            return dependency_error

        # 调用预测算法
        try:
            # 提取模型路径
            model_path = None
            if predict_request.model_selection:
                if predict_request.model_selection.get("mode") == "existing":
                    # 使用已有模型
                    model_info = predict_request.model_selection.get("model", {})
                    raw_model_path = model_info.get("path")
                    if not raw_model_path:
                        raise ValueError("使用已有模型时必须提供模型路径")

                    # 转换路径格式：/profile/xxx -> ./data/xxx
                    model_path = convert_profile_path(raw_model_path)

                elif predict_request.model_selection.get("mode") == "algorithm":
                    # 使用算法模式，需要从算法名称推断模型路径
                    algorithm = predict_request.model_selection.get("algorithm")
                    if algorithm:
                        # 这里可以根据算法名称查找对应的模型文件
                        # 暂时使用默认路径，实际应该从数据库或配置中获取
                        logging.warning(f"算法模式暂未实现模型路径查找: {algorithm}")
                        raise ValueError(f"算法模式 '{algorithm}' 的模型路径查找功能尚未实现")

            if not model_path:
                raise ValueError("预测任务必须指定模型路径")

            # 检查模型文件是否存在
            import os
            logging.info(f"🔍 检查模型文件: {model_path}")
            logging.info(f"🔍 当前工作目录: {os.getcwd()}")
            logging.info(f"🔍 文件是否存在: {os.path.exists(model_path)}")

            if not os.path.exists(model_path):
                # 尝试列出目录内容以帮助调试
                dir_path = os.path.dirname(model_path)
                if os.path.exists(dir_path):
                    files = os.listdir(dir_path)
                    logging.error(f"❌ 模型文件不存在: {model_path}")
                    logging.error(f"📁 目录 {dir_path} 中的文件: {files}")
                else:
                    logging.error(f"❌ 模型目录不存在: {dir_path}")
                raise FileNotFoundError(f"模型文件不存在: {model_path}")

            logging.info(f"✅ 使用模型路径: {model_path}")

            # 构建预测任务请求
            from app.schemas.task import TaskSubmitRequest

            # 将PredictRequest转换为TaskSubmitRequest格式
            task_request = TaskSubmitRequest(
                id=0,  # 预测任务使用临时ID
                task_name=predict_request.task_name,
                task_type=predict_request.task_type,
                algorithm=f"{predict_request.task_type}_{predict_request.model_selection.get('algorithm', 'linear')}_predict",
                input_params={
                    "model_path": model_path,  # 添加模型路径
                    "feature_columns": predict_request.features,  # 使用feature_columns而不是features
                    "target": predict_request.target,
                    "model_selection": predict_request.model_selection,
                    "parameters": predict_request.parameters or {},
                    "prediction_indices": predict_request.prediction_indices,
                    "sample_count": predict_request.sample_count,
                    "sampling_strategy": predict_request.sampling_strategy,
                    "output_precision": predict_request.output_precision,
                    "include_confidence": predict_request.include_confidence,
                    "include_input_features": predict_request.include_input_features,
                    "output_format": predict_request.output_format
                },
                input_file_path=extract_data_file_path(predict_request.data_file),
                output_dir_path="/tmp/prediction_output"
            )

            from app.services.processor import run_task

            logging.info(f"开始执行预测算法: {task_request.algorithm}")
            result = run_task(task_request)

            logging.info(f"✅ 预测算法执行成功")
            return create_success_response(
                data=result,
                message="预测执行成功",
                task_name=predict_request.task_name,
                features=predict_request.features,
                target=predict_request.target
            )

        except Exception as algorithm_error:
            error_message = f"预测算法执行失败: {str(algorithm_error)}"
            logging.error(f"❌ [错误码:{ErrorCodes.PREDICTION_FAILED}] {error_message}", exc_info=True)

            return create_error_response(
                error_code=ErrorCodes.PREDICTION_FAILED,
                message=error_message,
                task_name=predict_request.task_name,
                algorithm_error=True,
                details=str(algorithm_error)
            )

    except Exception as e:
        error_message = f"预测任务处理失败: {str(e)}"
        logging.error(f"❌ [错误码:{ErrorCodes.PREDICTION_FAILED}] {error_message}", exc_info=True)

        return create_error_response(
            error_code=ErrorCodes.PREDICTION_FAILED,
            message=error_message,
            exception=True
        )


@router.post("/batch-predict")
async def batch_predict_data(batch_request: Dict[str, Any]):
    """
    执行批量预测任务
    使用已训练的机器学习模型进行批量预测
    """
    try:
        logging.info(f"开始处理批量预测任务请求: {batch_request}")

        # 检查必需的依赖包
        dependency_error = check_ml_dependencies()
        if dependency_error:
            return dependency_error

        # 调用批量预测算法
        try:
            from app.schemas.task import TaskSubmitRequest

            # 构建批量预测任务请求
            task_request = TaskSubmitRequest(
                id=0,  # 批量预测任务使用临时ID
                task_name=batch_request.get("task_name", "批量预测任务"),
                task_type=batch_request.get("task_type", "batch_prediction"),
                algorithm=batch_request.get("algorithm", "batch_predict"),
                input_params=batch_request.get("input_params", {}),
                input_file_path=batch_request.get("input_file_path", ""),
                output_dir_path=batch_request.get("output_dir_path", "/tmp/batch_prediction_output")
            )

            from app.services.processor import run_task

            logging.info(f"开始执行批量预测算法: {task_request.algorithm}")
            result = run_task(task_request)

            logging.info(f"✅ 批量预测算法执行成功")
            return create_success_response(
                data=result,
                message="批量预测执行成功",
                batch_info=batch_request
            )

        except Exception as algorithm_error:
            error_message = f"批量预测算法执行失败: {str(algorithm_error)}"
            logging.error(f"❌ [错误码:{ErrorCodes.BATCH_PREDICTION_FAILED}] {error_message}", exc_info=True)

            return create_error_response(
                error_code=ErrorCodes.BATCH_PREDICTION_FAILED,
                message=error_message,
                algorithm_error=True,
                details=str(algorithm_error)
            )

    except Exception as e:
        error_message = f"批量预测任务处理失败: {str(e)}"
        logging.error(f"❌ [错误码:{ErrorCodes.BATCH_PREDICTION_FAILED}] {error_message}", exc_info=True)

        return create_error_response(
            error_code=ErrorCodes.BATCH_PREDICTION_FAILED,
            message=error_message,
            exception=True
        )


@router.get("/health")
async def health_check():
    """
    健康检查端点
    """
    return {
        "status": "healthy",
        "message": "分析服务运行正常，已集成真实机器学习算法",
        "mock_data_removed": True,
        "real_algorithms_enabled": True,
        "available_algorithms": [
            "regression_linear_train",
            "regression_polynomial_train",
            "regression_random_forest_train",
            "classification_logistic_train",
            "clustering_kmeans_train",
            "feature_engineering_automatic_regression_train"
        ]
    }


# 辅助函数：验证数据文件
def validate_data_file(data_file_info: Dict[str, Any]) -> bool:
    """
    验证数据文件信息的有效性
    """
    if not data_file_info:
        return False
    
    required_fields = ['name', 'path']
    for field in required_fields:
        if field not in data_file_info:
            logging.warning(f"数据文件信息缺少必需字段: {field}")
            return False
    
    return True


# 辅助函数：验证特征列
def validate_features(features: List[str], target: str) -> bool:
    """
    验证特征列和目标列的有效性
    """
    if not features:
        logging.error("特征列列表为空")
        return False
    
    if not target:
        logging.error("目标列为空")
        return False
    
    if target in features:
        logging.error(f"目标列 '{target}' 不能同时作为特征列")
        return False
    
    return True


# 辅助函数：记录请求信息
def log_request_info(request_type: str, request_data: Dict[str, Any]):
    """
    记录请求的详细信息
    """
    logging.info(f"=== {request_type} 请求详情 ===")
    logging.info(f"任务名称: {request_data.get('task_name', 'N/A')}")
    logging.info(f"任务类型: {request_data.get('task_type', 'N/A')}")
    logging.info(f"特征列: {request_data.get('features', [])}")
    logging.info(f"目标列: {request_data.get('target', 'N/A')}")
    
    if 'data_file' in request_data:
        logging.info(f"数据文件: {request_data['data_file']}")
    
    logging.info("=== 请求详情结束 ===")
