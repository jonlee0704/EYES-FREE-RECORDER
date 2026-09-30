#!/usr/bin/env python3
import os
import sys
import time
import subprocess
import shutil
from pathlib import Path

SRC_MUSIC = "/Volumes/Untitled/Android/data/com.jonlee.android.SJplayer/files/Music"
SRC_NOT_WORKING = "/Volumes/Untitled/not working"
DEST_BASE = "/storage/emulated/0/Android/data/com.jonlee.android.SJplayer/files/Music"
TMP_DIR = "/tmp/sjplayer_optimized_audio"

AUDIO_EXTS = {'.3gp', '.m4a', '.mp3', '.wav', '.mp4', '.aac', '.flac', '.ogg', '.mid', '.mkv', '.3ga', '.amr'}
CONVERT_EXTS = {'.wav', '.mp4'}
IGNORE_NAMES = {'.DS_Store', 'Archive.zip', 'Musicbacklog.zip', 'SJ-RECORDER-VALID-FOLDERS.txt'}

def run_cmd(cmd, check=True):
    return subprocess.run(cmd, capture_output=True, text=True, check=check)

def get_remote_dirs():
    """Get set of relative directories that already exist on the device."""
    res = subprocess.run([
        'adb', 'shell', 
        f'find "{DEST_BASE}" -mindepth 1 -type d'
    ], capture_output=True, text=True)
    dirs = set()
    for line in res.stdout.splitlines():
        line = line.strip()
        if line and line.startswith(DEST_BASE):
            rel = line[len(DEST_BASE):].strip('/')
            if rel:
                dirs.add(rel)
    return dirs

def get_remote_file_count(remote_dir):
    """Get number of audio files in a remote directory."""
    res = subprocess.run([
        'adb', 'shell',
        f'ls -1 "{remote_dir}" 2>/dev/null | wc -l'
    ], capture_output=True, text=True)
    try:
        return int(res.stdout.strip())
    except:
        return 0

def optimize_file(src_path, tmp_dir):
    """Converts .wav to .m4a or extracts .mp4 audio to .m4a."""
    ext = os.path.splitext(src_path)[1].lower()
    base_name = os.path.splitext(os.path.basename(src_path))[0]
    out_path = os.path.join(tmp_dir, base_name + ".m4a")
    
    if ext == '.wav':
        cmd = ['ffmpeg', '-y', '-i', src_path, '-vn', '-c:a', 'aac', '-b:a', '192k', out_path]
    elif ext == '.mp4':
        cmd = ['ffmpeg', '-y', '-i', src_path, '-vn', '-c:a', 'copy', out_path]
    else:
        return src_path, False
        
    subprocess.run(cmd, capture_output=True, check=True)
    return out_path, True

def sync_complex_folder(src_folder, rel_path, remote_dirs):
    """Sync a folder that contains .wav, .mp4, or nested folders file by file."""
    remote_folder = f"{DEST_BASE}/{rel_path}" if rel_path else DEST_BASE
    
    # Ensure remote directory exists
    if rel_path not in remote_dirs:
        run_cmd(['adb', 'shell', f'mkdir -p "{remote_folder}"'], check=False)
        remote_dirs.add(rel_path)
    
    # Get remote files in this directory
    res = subprocess.run(['adb', 'shell', f'ls -1 "{remote_folder}" 2>/dev/null'], capture_output=True, text=True)
    remote_files = set(res.stdout.splitlines())
    
    try:
        entries = os.listdir(src_folder)
    except Exception as e:
        print(f"  [WARN] Skipping unreadable dir: {src_folder} ({e})")
        return 0, 0

    transferred = 0
    bytes_transferred = 0
    
    for entry in entries:
        if entry.startswith('.') or entry in IGNORE_NAMES:
            continue
            
        full_path = os.path.join(src_folder, entry)
        if os.path.isdir(full_path):
            continue
            
        ext = os.path.splitext(entry)[1].lower()
        if ext not in AUDIO_EXTS:
            continue
            
        target_name = entry
        is_converted = False
        if ext in CONVERT_EXTS:
            target_name = os.path.splitext(entry)[0] + ".m4a"
            is_converted = True
            
        if target_name in remote_files:
            continue  # Already on device
            
        if is_converted:
            os.makedirs(TMP_DIR, exist_ok=True)
            try:
                opt_path, _ = optimize_file(full_path, TMP_DIR)
                run_cmd(['adb', 'push', opt_path, f"{remote_folder}/{target_name}"])
                sz = os.path.getsize(opt_path)
                os.remove(opt_path)
                transferred += 1
                bytes_transferred += sz
            except Exception as e:
                print(f"  [ERROR] Failed to optimize {full_path}: {e}")
        else:
            try:
                run_cmd(['adb', 'push', full_path, f"{remote_folder}/{target_name}"])
                sz = os.path.getsize(full_path)
                transferred += 1
                bytes_transferred += sz
            except Exception as e:
                print(f"  [ERROR] Failed to push {full_path}: {e}")
                
    return transferred, bytes_transferred

def main():
    if not os.path.exists(SRC_MUSIC):
        print(f"Error: Source directory {SRC_MUSIC} not found!")
        sys.exit(1)
        
    os.makedirs(TMP_DIR, exist_ok=True)
    
    print("Fetching remote directories from Samsung device...")
    remote_dirs = get_remote_dirs()
    print(f"Found {len(remote_dirs)} existing folders on device.")
    
    # 1. Discover all folders in SRC_MUSIC
    print("Scanning folders to sync...")
    top_items = sorted(os.listdir(SRC_MUSIC))
    
    # Filter out junk
    folders_to_sync = []
    for item in top_items:
        if item.startswith('.') or item in IGNORE_NAMES:
            continue
        p = os.path.join(SRC_MUSIC, item)
        if os.path.isdir(p):
            folders_to_sync.append(item)
            
    total_folders = len(folders_to_sync)
    print(f"Found {total_folders} top-level folders to process.")
    
    start_time = time.time()
    total_files_transferred = 0
    total_bytes_transferred = 0
    
    for idx, folder_name in enumerate(folders_to_sync, 1):
        folder_path = os.path.join(SRC_MUSIC, folder_name)
        
        # Check if this folder has nested dirs, wav, or mp4
        has_convert = False
        has_subdirs = False
        audio_count = 0
        
        try:
            entries = os.listdir(folder_path)
        except Exception as e:
            print(f"[{idx}/{total_folders}] Skipping {folder_name} (unreadable: {e})")
            continue
            
        for f in entries:
            fp = os.path.join(folder_path, f)
            if os.path.isdir(fp):
                has_subdirs = True
            ext = os.path.splitext(f)[1].lower()
            if ext in CONVERT_EXTS:
                has_convert = True
            if ext in AUDIO_EXTS:
                audio_count += 1
                
        remote_path = f"{DEST_BASE}/{folder_name}"
        
        if not has_convert and not has_subdirs:
            # Simple flat folder with pure audio
            if folder_name in remote_dirs:
                # Check if file counts match
                rcnt = get_remote_file_count(remote_path)
                if rcnt >= audio_count and audio_count > 0:
                    # Already synced
                    if idx % 100 == 0 or idx == total_folders:
                        print(f"[{idx}/{total_folders}] {folder_name} - Already synced ({rcnt} files)")
                    continue
                    
            # Push folder directly
            t0 = time.time()
            res = subprocess.run(['adb', 'push', folder_path, DEST_BASE + '/'], capture_output=True, text=True)
            elapsed = time.time() - t0
            remote_dirs.add(folder_name)
            total_files_transferred += audio_count
            print(f"[{idx}/{total_folders}] {folder_name} ({audio_count} files) pushed in {elapsed:.1f}s")
        else:
            # Complex folder (e.g. SJ-RECORDER-SHARED-FOLDER or folders with wav/mp4/nested dirs)
            print(f"[{idx}/{total_folders}] Processing complex folder: {folder_name} (convert={has_convert}, subdirs={has_subdirs})")
            for root, dirs, files in os.walk(folder_path):
                rel = os.path.relpath(root, SRC_MUSIC)
                cnt, b = sync_complex_folder(root, rel, remote_dirs)
                total_files_transferred += cnt
                total_bytes_transferred += b
                
    # Also sync not working folder if exists
    if os.path.exists(SRC_NOT_WORKING):
        print("Processing 'not working' folder...")
        sync_complex_folder(SRC_NOT_WORKING, "not working", remote_dirs)
        
    shutil.rmtree(TMP_DIR, ignore_errors=True)
    
    total_elapsed = time.time() - start_time
    print(f"\n==========================================")
    print(f"Sync complete in {total_elapsed/60:.1f} minutes!")
    print(f"Transferred: {total_files_transferred} files, {total_bytes_transferred/(1024**2):.1f} MB (via complex sync)")
    print(f"==========================================")
    
    # Restart app to trigger folder indexing
    print("Restarting EYES-Free recorder to index audio folders...")
    subprocess.run(['adb', 'shell', 'am', 'force-stop', 'com.jonlee.android.SJplayer'])
    subprocess.run(['adb', 'shell', 'am', 'start', '-n', 'com.jonlee.android.SJplayer/.MainActivity'])
    print("Done!")

if __name__ == '__main__':
    main()
