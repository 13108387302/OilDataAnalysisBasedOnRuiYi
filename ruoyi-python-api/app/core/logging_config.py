import logging
from pathlib import Path

def setup_file_logging():
    """
    配置日志系统，将日志输出到指定目录。
    """
    # 定义日志目录，与Java后端保持一致
    LOG_DIR = Path("E:/Code/RuoYi_/RuoYi-Vue/logs")
    LOG_DIR.mkdir(exist_ok=True)

    # 定义不同类型的日志文件
    app_log_file = LOG_DIR / "python-api.log"
    error_log_file = LOG_DIR / "python-error.log"

    # 获取根日志记录器
    logger = logging.getLogger()
    logger.setLevel(logging.INFO)

    # 创建一个通用的格式化器
    formatter = logging.Formatter(
        "%(asctime)s - [%(levelname)s] - %(name)s - (%(filename)s).%(funcName)s(%(lineno)d) - %(message)s"
    )

    # 移除所有现有的处理器，以确保我们的配置是唯一的
    for handler in logger.handlers[:]:
        logger.removeHandler(handler)

    # --- 控制台处理器 ---
    # 始终将日志输出到控制台
    stream_handler = logging.StreamHandler()
    stream_handler.setFormatter(formatter)
    stream_handler.setLevel(logging.INFO)
    logger.addHandler(stream_handler)

    # --- 应用日志文件处理器 (覆盖模式) ---
    app_file_handler = logging.FileHandler(
        app_log_file,
        mode='w',  # 覆盖模式
        encoding='utf-8'
    )
    app_file_handler.setLevel(logging.INFO)
    app_file_handler.setFormatter(formatter)
    logger.addHandler(app_file_handler)

    # --- 错误日志文件处理器 (覆盖模式) ---
    error_file_handler = logging.FileHandler(
        error_log_file,
        mode='w',  # 覆盖模式
        encoding='utf-8'
    )
    error_file_handler.setLevel(logging.ERROR)
    error_file_handler.setFormatter(formatter)
    logger.addHandler(error_file_handler)

    logging.info(f"Python API日志已配置。日志文件位于: {LOG_DIR}")
    logging.info(f"应用日志: {app_log_file}")
    logging.info(f"错误日志: {error_log_file}")