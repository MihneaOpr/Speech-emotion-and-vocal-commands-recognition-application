import torch
from torch.utils.data import Dataset
from transformers import AutoFeatureExtractor
import os
import librosa

SR = 16e3
MAXIMUM_LEN = 1e5


class SpeechDataset(Dataset):
    def __init__(self, data_dir):
        self.labels = ['W', 'L', 'E', 'A', 'F', 'T', 'N']
        self.label2id = {}
        self.id2label = {}
        for (idx, label) in enumerate(self.labels):
            self.label2id[label] = idx
            self.id2label[idx] = label

        self.data = self.process_speech(data_dir)

    def __getitem__(self, item):
        return self.data[item]

    def process_speech(self, data_dir):
        feature_extractor = AutoFeatureExtractor.from_pretrained("facebook/wav2vec2-base")
        data = []
        for filename in os.listdir(data_dir):
            speech_file = os.path.join(data_dir, filename)
            speech, sample_rate = librosa.load(speech_file, sr=SR)
            label = filename.split('.')[0][-2]
            input_ids = feature_extractor(speech, sampling_rate=sample_rate, return_tensors="pt", padding=True)
            input_ids['labels'] = self.label2id[label]
            input_ids['attention_mask'] = torch.ones_like(input_ids['input_values'])
            data.append(input_ids)
        return data

    def num_labels(self):
        return len(self.labels)

    def __len__(self):
        return len(self.data)
