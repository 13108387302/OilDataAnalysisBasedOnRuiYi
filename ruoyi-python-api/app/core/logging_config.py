import logging
from logging import FileHandler
from pathlib import Path

def setup_file_logging():
    """
    配置日志系统，将日志输出到文件，每次启动时覆盖。
    """
    # 定义日志目录，位于项目根目录下的 logs/
    # __file__ -> logging_config.py
    # .parent -> core
    # .parent -> app
    # .parent -> ruoyi-python-api
    # .parent -> RuoYi-Vue (项目根目录)
    LOG_DIR = Path(__file__).resolve().parent.parent.parent.parent / "logs"
    LOG_DIR.mkdir(exist_ok=True)
    log_file_path = LOG_DIR / "python_api.log"

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
    
    # --- 文件处理器 (覆盖模式) ---
    # mode='w' 表示写入模式，会覆盖旧文件
    # 创建文件处理器，设置为覆盖模式 ('w')
    # 使用 RotatingFileHandler 并设置 backupCount=0 也可以实现启动时覆盖
    # 但直接使用 FileHandler(mode='w') 更直接
    file_handler = logging.FileHandler(log_file_path, mode='w', encoding='utf-8')
    file_handler.setLevel(logging.INFO)
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)

    logging.info(f"文件日志已配置。日志文件位于: {log_file_path}")