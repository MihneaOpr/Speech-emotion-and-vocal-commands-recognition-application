import requests

url = 'http://127.0.0.1:5000/upload'
file_path = r'C:\Users\mihne\OneDrive\Desktop\TEST\recording_1_hp.wav'

with open(file_path, 'rb') as file:
    response = requests.post(url, files={'file': file})

if response.status_code == 200:
    print('File uploaded successfully.')
    print('Response:', response.json())
else:
    print('File upload failed.')
    print('Response:', response.json())
