import React, { useRef } from 'react';
import { X, Cpu, FolderOpen, RefreshCw, CheckCircle2, AlertTriangle } from 'lucide-react';

interface ModelManagerModalProps {
  isOpen: boolean;
  onClose: () => void;
  isModelLoaded: boolean;
  modelName: string;
  modelSizeMb: string;
  onLoadCustomFile: (file: File) => void;
  onReloadDefaultAssets: () => void;
}

export const ModelManagerModal: React.FC<ModelManagerModalProps> = ({
  isOpen,
  onClose,
  isModelLoaded,
  modelName,
  modelSizeMb,
  onLoadCustomFile,
  onReloadDefaultAssets,
}) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="model-dialog-title"
    >
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-emerald-500/15 text-emerald-400">
              <Cpu className="w-5 h-5" />
            </div>
            <div>
              <h2 id="model-dialog-title" className="text-lg font-bold text-white">
                Gerenciador do Modelo YOLO
              </h2>
              <p className="text-xs text-slate-400">
                LiteRT / TensorFlow Lite • Entrada [1, 3, 640, 640] • 23 Classes
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar gerenciador de modelo"
            className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="py-5 space-y-4">
          <div className="p-4 rounded-xl bg-slate-800/80 border border-slate-700/70 space-y-1.5">
            <div className="flex items-center gap-2">
              {isModelLoaded ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              ) : (
                <AlertTriangle className="w-4 h-4 text-amber-400" />
              )}
              <span
                className={`text-sm font-bold ${
                  isModelLoaded ? 'text-emerald-400' : 'text-amber-400'
                }`}
              >
                Status: {isModelLoaded ? 'Ativo (LiteRT GPU/CPU Delegate)' : 'Não Carregado'}
              </span>
            </div>
            <p className="text-xs text-slate-300">
              Arquivo: <span className="font-mono text-white">{modelName || 'Nenhum'}</span> (
              {modelSizeMb})
            </p>
            <p className="text-[11px] text-slate-400">
              Tensores de Saída: Detecções <span className="font-mono">[1, 59, 8400]</span> +
              Protótipos de Máscara <span className="font-mono">[1, 32, 160, 160]</span>
            </p>
          </div>

          <div className="space-y-2 text-xs text-slate-300 leading-relaxed">
            <p className="font-semibold text-white">Como usar o modelo treinado:</p>
            <p>
              1. O modelo <span className="font-mono text-blue-300">best.tflite</span> (10.7 MB)
              exportado no Google Colab a partir do treinamento de 50 épocas em{' '}
              <span className="font-mono text-blue-300">carparts-seg.yaml</span> já está pré-carregado
              nos assets.
            </p>
            <p>
              2. Você também pode carregar qualquer arquivo{' '}
              <span className="font-mono text-blue-300">.tflite</span> customizado do seu
              armazenamento local para inspecionar seus pesos.
            </p>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            accept=".tflite,application/octet-stream"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) {
                onLoadCustomFile(file);
              }
            }}
          />

          <div className="space-y-2.5 pt-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold transition-colors"
            >
              <FolderOpen className="w-4 h-4" />
              Selecionar best.tflite do Armazenamento
            </button>

            <button
              type="button"
              onClick={onReloadDefaultAssets}
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-sm font-semibold transition-colors"
            >
              <RefreshCw className="w-4 h-4" />
              Recarregar Modelo Padrão dos Assets (best.tflite)
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
