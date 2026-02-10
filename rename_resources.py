import os

drawable_path = r"C:\Users\Vitos\AndroidStudioProjects\sem6_jrzpo\minesveeper_kotlin\app\src\main\res\drawable"

print(f"Starting total resource cleanup in: {drawable_path}")

allowed_extensions = {".png", ".xml", ".jpg", ".webp"}

for filename in os.listdir(drawable_path):
    filepath = os.path.join(drawable_path, filename)
    
    if os.path.isdir(filepath):
        continue
        
    ext = os.path.splitext(filename)[1].lower()
    
    # Если расширение не разрешено - удаляем файл
    if ext not in allowed_extensions:
        try:
            os.remove(filepath)
            print(f"  - Deleted non-resource file: {filename}")
        except Exception as e:
            print(f"  - Could not delete {filename}: {e}")
            
    # Принудительно в нижний регистр (через временный файл для Windows)
    new_filename = filename.lower().replace("-", "_")
    if new_filename != filename:
        new_filepath = os.path.join(drawable_path, new_filename)
        temp_filepath = filepath + ".tmp"
        try:
            os.rename(filepath, temp_filepath)
            if os.path.exists(new_filepath):
                os.remove(new_filepath)
            os.rename(temp_filepath, new_filepath)
            print(f"  - Fixed name/case: {filename} -> {new_filename}")
        except Exception as e:
            print(f"  - Error renaming {filename}: {e}")

print("\nCleanup finished! Now your drawable folder is clean and valid for Android.")
