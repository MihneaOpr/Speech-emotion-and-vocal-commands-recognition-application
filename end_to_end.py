from transformers import pipeline, AutoModelForAudioClassification, AutoFeatureExtractor
from speech_dataset_english import  SpeechDataset

model = AutoModelForAudioClassification.from_pretrained('./best_models')

feature_extractor = AutoFeatureExtractor.from_pretrained("facebook/wav2vec2-base")
classifier = pipeline('audio-classification', model=model, feature_extractor=feature_extractor)

dataset = SpeechDataset('wav2')

audio_file = dataset.__getitem__(139)


print(f'True label: {audio_file["labels"]}')
print(classifier(audio_file['input_values'][0].numpy()))
