import uvicorn
from fastapi import FastAPI
from app.core.logging_config import setup_file_logging

# 在应用启动前加载日志配置
setup_file_logging()

# 创建 FastAPI 应用实例
app = FastAPI(
    title="石油参数预测系统 - AI 服务",
    description="提供数据处理、模型训练和预测的 API 服务。",
    version="1.0.0"
)

# 定义一个根路径的 "hello world"
@app.get("/")
def read_root():
    return {"message": "欢迎使用石油参数预测 AI 服务"}

# 在这里，我们未来会包含来自其他模块的 API 路由
from app.api.endpoints import analysis
app.include_router(analysis.router, prefix="/api/v1", tags=["Analysis Tasks"])

if __name__ == "__main__":
    # 这个部分允许我们像运行普通 Python 脚本一样直接启动服务，方便调试
    # 在生产环境中，我们会使用 uvicorn 命令来启动
    # 🔧 修复：修改端口为8000以匹配Java后端配置
    uvicorn.run(app, host="0.0.0.0", port=8000)