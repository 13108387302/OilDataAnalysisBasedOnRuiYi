from typing import Optional, Dict, Any
from pydantic import BaseModel, Field

class TaskSubmitRequest(BaseModel):
    """
    任务提交 API 的请求体模型
    """
    id: int = Field(..., example=101, description="由Java后端生成的数据库任务ID")
    # Field 用于提供额外的模型信息，例如在 Swagger UI 中显示的示例
    task_name: str = Field(..., example="我的第一个预测任务")
    task_type: str = Field(..., example="predict")
    algorithm: str = Field(..., example="predict_linear_regression")
    
    # 这里的 input_params_json 对应原项目中的同名参数
    # 它是一个字典，包含了算法需要的各种超参数
    input_params: Dict[str, Any] = Field(..., example={"degree": 2, "test_size": 0.2})
    
    # 文件路径将由 Java 端处理后，通过 API 传递过来
    # 在新架构中，Java 端仍然负责文件的上传和存储
    input_file_path: str = Field(..., example="E:/Code/RuoYi_/RuoYi-Vue/data/petrol/uploads/predict_linear_regression/1625123456789/my_data.xlsx")
    output_dir_path: str = Field(..., example="E:/Code/RuoYi_/RuoYi-Vue/data/petrol/results/predict_linear_regression/1625123456789")

class TaskSubmitResponse(BaseModel):
    """
    任务提交 API 的响应体模型
    """
    message: str = Field(..., example="任务提交成功")
    task_id: str = Field(..., example="celery-task-id-abcdef123456") 