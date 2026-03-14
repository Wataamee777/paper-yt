import os
from yt_dlp import YoutubeDL

# progress_callback を引数に追加
def run_ytdlp(args_list, download_dir, ffmpeg_path, progress_callback):
    if not os.path.exists(download_dir):
        os.makedirs(download_dir, exist_ok=True)

    ffmpeg_dir = os.path.dirname(ffmpeg_path)
    os.environ["PATH"] = ffmpeg_dir + os.pathsep + os.environ["PATH"]

    # yt-dlpのフックに関数を登録
    def internal_hook(d):
        if d['status'] == 'downloading':
            # パーセント数値を抽出してKotlin側に送る
            p = d.get('_percent_str', '0%').replace('%','')
            try:
                progress_callback.onProgress(float(p))
            except:
                pass
        elif d['status'] == 'finished':
            progress_callback.onProgress(100.0)

    ydl_opts = {
        'paths': {'home': download_dir},
        'ffmpeg_location': ffmpeg_path, 
        'progress_hooks': [internal_hook], # 内部フックを使用
        'prefer_ffmpeg': True,
    }

    try:
        with YoutubeDL(ydl_opts) as ydl:
            ydl.download(args_list)
        return "Download Completed!"
    except Exception as e:
        return f"Python Error: {str(e)}"