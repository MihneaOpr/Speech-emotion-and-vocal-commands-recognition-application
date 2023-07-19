from flask import Flask, request, jsonify
import librosa
from transformers import AutoModelForAudioClassification, AutoFeatureExtractor
import torch
import os
import wave

from werkzeug.datastructures import FileStorage
import io

from speech_dataset_autovoice import SpeechDatasetAutovoice
from speech_dataset_english import SpeechDataset

app = Flask(__name__)

# Load the speech recognition model
dataset1 = SpeechDataset('wav2')
dataset2 = SpeechDatasetAutovoice('wav3')
model1 = AutoModelForAudioClassification.from_pretrained("best_models", num_labels=8)
model2 = AutoModelForAudioClassification.from_pretrained("best_models_autovoice", num_labels=9)
feature_extractor = AutoFeatureExtractor.from_pretrained("facebook/wav2vec2-base")
SR = 16e3


def get_evaluation(audio_file, filename):
    try:
        if filename.startswith('recording_1'):
            speech, sample_rate = librosa.load(audio_file, sr=44e3)
            speech_down_sampled = librosa.resample(speech, orig_sr=44e3, target_sr=SR)

            input_ids = feature_extractor(speech_down_sampled, sampling_rate=SR, return_tensors="pt", padding=True)
            input_ids['attention_mask'] = torch.ones_like(input_ids['input_values'])

            with torch.no_grad():
                logits = model1(**input_ids).logits

            percentage = torch.nn.functional.softmax(logits, dim=1)[0]
            result = {}
            for (key, value) in dataset1.id2label.items():
                result[value] = round(percentage[key].item() * 100, 2)
            return result
        elif filename.startswith('recording_2'):
            speech, sample_rate = librosa.load(audio_file, sr=44e3)
            speech_down_sampled = librosa.resample(speech, orig_sr=44e3, target_sr=SR)

            input_ids = feature_extractor(speech_down_sampled, sampling_rate=SR, return_tensors="pt", padding=True)
            input_ids['attention_mask'] = torch.ones_like(input_ids['input_values'])

            with torch.no_grad():
                logits = model2(**input_ids).logits

            percentage = torch.nn.functional.softmax(logits, dim=1)[0]
            result = {}
            for (key, value) in dataset2.id2label.items():
                result[value] = round(percentage[key].item() * 100, 2)
            return result
    except Exception as e:
        print(f"Error processing audio: {str(e)}")
        return None


def convert_raw_to_wav(raw_filestorage):
    # Read the raw audio data from the FileStorage object
    raw_data = raw_filestorage.read()

    # Create a new WAV file in memory
    wav_file = io.BytesIO()
    with wave.open(wav_file, 'wb') as wav:
        # Set the WAV file parameters based on the raw audio data
        wav.setnchannels(1)  # Mono
        wav.setsampwidth(2)  # 2 bytes per sample (16-bit)
        wav.setframerate(44e3)  # Sample rate (e.g., 44kHz)

        # Write the raw audio data to the WAV file
        wav.writeframes(raw_data)

    # Rewind the WAV file to the beginning
    wav_file.seek(0)

    # Create a new FileStorage object to store the WAV file
    wav_filestorage = FileStorage(wav_file, filename=raw_filestorage.filename,
                                  content_type=raw_filestorage.content_type,
                                  content_length=raw_filestorage.content_length,
                                  headers=raw_filestorage.headers,
                                  name='file')

    return wav_filestorage


@app.route('/upload', methods=['POST'])
def upload():
    if 'file' not in request.files:
        return jsonify({'error': 'No file uploaded'}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400

    print(f"Received file: {file.filename}")

    if file and allowed_file(file.filename):
        try:
            # Save the WAV file with a unique filename
            filename = generate_unique_filename(file.filename)

            file = convert_raw_to_wav(file)
            file.save(filename)

            if model1 is None and model2 is None:
                return jsonify({'error': 'Invalid file format'}), 400

            # Process the WAV file
            response = get_evaluation(filename, file.filename)

            # Delete the temporary WAV file
            os.remove(filename)

            if response is None:
                return jsonify({'error': 'Error processing audio'}), 500

            return jsonify(response), 200
        except Exception as e:
            print(f"Error handling audio upload: {str(e)}")
            return jsonify({'error': 'An error occurred'}), 500
    else:
        return jsonify({'error': 'Invalid file format'}), 400


def generate_unique_filename(original_filename):
    _, ext = os.path.splitext(original_filename)
    filename = f"temp_{os.urandom(6).hex()}{ext}"
    # Ensure the generated filename doesn't already exist
    while os.path.exists(filename):
        filename = f"temp_{os.urandom(6).hex()}{ext}"
    return filename


def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() == 'wav'


if __name__ == '__main__':
    try:
        app.run()
    except Exception as e:
        print(f"Error running Flask application: {str(e)}")
