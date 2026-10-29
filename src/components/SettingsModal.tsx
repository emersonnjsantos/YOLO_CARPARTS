import React from 'react';
import { X, Sliders, RotateCcw } from 'lucide-react';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  confThreshold: number;
  iouThreshold: number;
  maskOpacity: number;
  showMasks: boolean;
  showBoxes: boolean;
  showLabels: boolean;
  onConfChange: (val: number) => void;
  onIouChange: (val: number) => void;
  onOpacityChange: (val: number) => void;
  onToggleMasks: (val: boolean) => void;
  onToggleBoxes: (val: boolean) => void;
  onToggleLabels: (val: boolean) => void;
  onResetDefaults: () => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  confThreshold,
  iouThreshold,
  maskOpacity,
  showMasks,
  showBoxes,
  showLabels,
  onConfChange,
  onIouChange,
  onOpacityChange,
  onToggleMasks,
  onToggleBoxes,
  onToggleLabels,
  onResetDefaults,
}) => {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="settings-dialog-title"
    >
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-blue-500/15 text-blue-400">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h2 id="settings-dialog-title" className="text-lg font-bold text-white">
                Configurações de Detecção
              </h2>
              <p className="text-xs text-slate-400">
                Hiperparâmetros em tempo real do YoloSegmenter (LiteRT)
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar configurações"
            className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-5 py-5">
          {/* Confidence Threshold */}
          <div>
            <div className="flex justify-between text-sm font-semibold mb-1.5">
              <label htmlFor="conf-slider" className="text-slate-200">
                Confiança Mínima
              </label>
              <span className="text-blue-400 font-mono">{Math.round(confThreshold * 100)}%</span>
            </div>
            <input
              id="conf-slider"
              type="range"
              min={0.1}
              max={0.9}
              step={0.05}
              value={confThreshold}
              onChange={(e) => onConfChange( parseFloat(e.target.value) )}
              className="w-full accent-blue-500 cursor-pointer"
            />
            <div className="flex justify-between text-[11px] text-slate-500 mt-0.5">
              <span>10% (Mais sensível)</span>
              <span>Padrão: 25%</span>
              <span>90% (Mais restrito)</span>
            </div>
          </div>

          {/* IoU NMS Threshold */}
          <div>
            <div className="flex justify-between text-sm font-semibold mb-1.5">
              <label htmlFor="iou-slider" className="text-slate-200">
                Limiar de Supressão (IoU NMS)
              </label>
              <span className="text-blue-400 font-mono">{Math.round(iouThreshold * 100)}%</span>
            </div>
            <input
              id="iou-slider"
              type="range"
              min={0.2}
              max={0.8}
              step={0.05}
              value={iouThreshold}
              onChange={(e) => onIouChange(parseFloat(e.target.value))}
              className="w-full accent-blue-500 cursor-pointer"
            />
          </div>

          {/* Mask Opacity */}
          <div>
            <div className="flex justify-between text-sm font-semibold mb-1.5">
              <label htmlFor="opacity-slider" className="text-slate-200">
                Opacidade das Máscaras
              </label>
              <span className="text-blue-400 font-mono">
                {Math.round((maskOpacity / 255) * 100)}%
              </span>
            </div>
            <input
              id="opacity-slider"
              type="range"
              min={40}
              max={255}
              step={5}
              value={maskOpacity}
              onChange={(e) => onOpacityChange(parseInt(e.target.value, 10))}
              className="w-full accent-blue-500 cursor-pointer"
            />
          </div>

          {/* Visual layer switches */}
          <div className="space-y-3 pt-2 border-t border-slate-800">
            <label className="flex items-center justify-between cursor-pointer py-1">
              <span className="text-sm text-slate-200">Mostrar Máscaras de Segmentação</span>
              <input
                type="checkbox"
                checked={showMasks}
                onChange={(e) => onToggleMasks(e.target.checked)}
                className="w-4 h-4 accent-blue-500 rounded"
              />
            </label>

            <label className="flex items-center justify-between cursor-pointer py-1">
              <span className="text-sm text-slate-200">Mostrar Bounding Boxes</span>
              <input
                type="checkbox"
                checked={showBoxes}
                onChange={(e) => onToggleBoxes(e.target.checked)}
                className="w-4 h-4 accent-blue-500 rounded"
              />
            </label>

            <label className="flex items-center justify-between cursor-pointer py-1">
              <span className="text-sm text-slate-200">Mostrar Nomes e Confiança</span>
              <input
                type="checkbox"
                checked={showLabels}
                onChange={(e) => onToggleLabels(e.target.checked)}
                className="w-4 h-4 accent-blue-500 rounded"
              />
            </label>
          </div>
        </div>

        <div className="flex items-center justify-between pt-4 border-t border-slate-800">
          <button
            type="button"
            onClick={onResetDefaults}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Restaurar Padrões
          </button>
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 rounded-xl text-sm font-semibold bg-blue-600 hover:bg-blue-500 text-white transition-colors"
          >
            Concluído
          </button>
        </div>
      </div>
    </div>
  );
};
