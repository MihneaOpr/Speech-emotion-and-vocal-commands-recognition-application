import torch

from speech_dataset_english import SpeechDataset
from speech_dataset_autovoice import SpeechDatasetAutovoice
from torch.utils.data import DataLoader, random_split
from transformers import AutoModelForAudioClassification, TrainingArguments, Trainer, AutoFeatureExtractor
from speech_datacollator import collate_batch
from sklearn.metrics import accuracy_score
from transformers import pipeline
import librosa
import torch.nn as nn


def compute_metrics(pred):
    labels = pred.label_ids
    preds = pred.predictions.argmax(-1)
    acc = accuracy_score(labels, preds)
    return {"accuracy": acc}


BATCH_SIZE = 4
VAL_SPLIT = 0.2

if __name__ == '__main__':
    torch.manual_seed(42)

    dataset = SpeechDatasetAutovoice('wav3')

    feature_extractor = AutoFeatureExtractor.from_pretrained("facebook/wav2vec2-base")

    # Compute the split index
    val_size = int(VAL_SPLIT * len(dataset))
    train_size = len(dataset) - val_size

    train_dataset, val_dataset = random_split(dataset, [train_size, val_size])

    # train_dataloader = DataLoader(dataset=train_dataset, batch_size=BATCH_SIZE, shuffle=True)
    # val_dataloader = DataLoader(dataset=val_dataset, batch_size=BATCH_SIZE, shuffle=False)

    model = AutoModelForAudioClassification.from_pretrained("facebook/wav2vec2-base",
                                                            num_labels=dataset.num_labels())

    # Define the training arguments
    training_args = TrainingArguments(
        output_dir="./output_dir",  # Directory to save checkpoints and logs
        num_train_epochs=10,  # Number of training epochs
        per_device_train_batch_size=BATCH_SIZE,  # Batch size per GPU for training
        per_device_eval_batch_size=BATCH_SIZE,  # Batch size per GPU for evaluation
        evaluation_strategy="epoch",  # Evaluate after each epoch
        save_strategy="epoch",  # Save checkpoint after each epoch
        logging_dir="./logs",  # Directory to save logs
        seed=42,
        load_best_model_at_end=True,
        metric_for_best_model='accuracy',
        logging_steps=10,
        warmup_ratio=0.1
    )

    # Create a Trainer instance
    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=train_dataset,
        eval_dataset=val_dataset,
        data_collator=collate_batch,
        compute_metrics=compute_metrics,
    )

    trainer_device = trainer.model.device
    print(f'Training on: {trainer_device}')
    # Fine-tune the model
    trainer.train()
    trainer.save_model('best_models_autovoice')
