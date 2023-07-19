import torch
from torch.utils.data import Dataset
from transformers import AutoFeatureExtractor
import os
import librosa

SR = 16e3


class SpeechDatasetAutovoice(Dataset):
    def __init__(self, data_dir):
        self.labels = [label for label in os.listdir(data_dir)]
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
        for label in os.listdir(data_dir):
            label_files = os.path.join(data_dir, label)
            for audio_name in os.listdir(label_files):
                speech_file = os.path.join(label_files, audio_name)
                speech, sample_rate = librosa.load(speech_file, sr=44e3)
                speech_down_sampled = librosa.resample(speech, orig_sr=44e3, target_sr=SR)
                input_ids = feature_extractor(speech_down_sampled, sampling_rate=SR, return_tensors="pt", padding=True)
                input_ids['labels'] = self.label2id[label]
                input_ids['attention_mask'] = torch.ones_like(input_ids['input_values'])
                data.append(input_ids)
        return data

    def num_labels(self):
        return len(self.labels)

    def __len__(self):
        return len(self.data)


if __name__ == '__main__':
    dataset = SpeechDatasetAutovoice('wav3')
