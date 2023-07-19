from transformers import DataCollator
from torch.nn.utils.rnn import pad_sequence
import torch


def collate_batch(features):
    input_values = [feature["input_values"][0] for feature in features]
    attention_masks = [feature["attention_mask"][0] for feature in features]
    labels = [feature["labels"] for feature in features]

    padded_input_values = pad_sequence(input_values, batch_first=True)
    padded_attention_masks = pad_sequence(attention_masks, batch_first=True)

    return {
        "input_values": padded_input_values,
        "attention_mask": padded_attention_masks,
        "labels": torch.tensor(labels)
    }
