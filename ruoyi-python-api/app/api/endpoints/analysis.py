import logging
from fastapi import APIRouter, HTTPException
from app.schemas.task import TaskSubmitRequest
from app.services import processor

# 创建一个 API 路由，它可以被主应用 app/main.py 包含进去
router = APIRouter()

@router.post("/submit")
def submit_analysis_task(task_request: TaskSubmitRequest):
    """
    提交一个新的分析/预测任务。

    - **task_request**: 包含任务所有信息的请求体。
    """
    logging.info(f"接收到新任务: {task_request.task_name}，算法: {task_request.algorithm}")

    # TODO: 这里是未来集成 Celery 的地方
    # 1. 调用 celery_task.delay(task_request.dict())
    # 2. 获取返回的 task_id

    # --- 当前的伪实现 ---
    # 在这里，我们将直接调用重构后的处理器逻辑
    try:
        logging.info(f"开始处理任务请求: {task_request.model_dump()}")

        # 调用处理器服务来执行任务
        result = processor.run_task(task_request)
        logging.info(f"任务 '{task_request.task_name}' 执行完成，结果类型: {type(result)}")
        logging.info(f"任务 '{task_request.task_name}' 执行完成，结果: {result}")

        # 返回实际的分析结果，而不是任务提交确认
        return result

    except Exception as e:
        logging.error(f"任务执行失败: {e}", exc_info=True)
        logging.error(f"任务请求数据: {task_request.model_dump()}")
        raise HTTPException(status_code=500, detail=f"任务执行失败: {str(e)}")
    # --------------------