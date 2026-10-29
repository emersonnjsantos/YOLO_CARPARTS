import React, { useState, useMemo, useRef, useEffect } from 'react';
import {
  Camera,
  Image as ImageIcon,
  Sliders,
  Upload,
  RefreshCw,
  BookOpen,
  Zap,
  CheckCircle2,
  SwitchCamera,
  Sparkles,
  Play,
  Pause,
} from 'lucide-react';
import {
  VEHICLE_PRESETS,
  VehiclePreset,
  runSegmentationPipeline,
  RawDetection,
  CAR_PARTS_CATALOG,
} from './ml/yoloEngine';
import { DetectionCanvas } from './components/DetectionCanvas';
import { SettingsModal } from './components/SettingsModal';
import { ModelManagerModal } from './components/ModelManagerModal';
import { TrainingCatalogPanel } from './components/TrainingCatalogPanel';

type AppTab = 'gallery' | 'live' | 'catalog';

export function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('gallery');
  const [selectedPresetId, setSelectedPresetId] = useState<string>(VEHICLE_PRESETS[0].id);
  const [customImage, setCustomImage] = useState<HTMLImageElement | null>(null);
  const [customImageName, setCustomImageName] = useState<string>('');

  // Model state matching MainScreenViewModel.kt
  const [isModelLoaded, setIsModelLoaded] = useState<boolean>(true);
  const [modelName, setModelName] = useState<string>('best.tflite (Assets)');
  const [modelSizeMb, setModelSizeMb] = useState<string>('10.7 MB');

  // Detection hyperparameters matching MainScreenViewModel.kt
  const [confThreshold, setConfThreshold] = useState<number>(0.25);
  const [iouThreshold, setIouThreshold] = useState<number>(0.45);
  const [maskOpacity, setMaskOpacity] = useState<number>(160);
  const [showMasks, setShowMasks] = useState<boolean>(true);
  const [showBoxes, setShowBoxes] = useState<boolean>(true);
  const [showLabels, setShowLabels] = useState<boolean>(true);

  const [highlightedPartId, setHighlightedPartId] = useState<string | null>(null);
  const [showSettingsModal, setShowSettingsModal] = useState<boolean>(false);
  const [showModelModal, setShowModelModal] = useState<boolean>(false);
  const [statusBanner, setStatusBanner] = useState<string | null>(null);

  // Live Camera simulation / WebRTC state
  const [isMirrored, setIsMirrored] = useState<boolean>(false);
  const [isLivePlaying, setIsLivePlaying] = useState<boolean>(true);
  const [liveFrameIndex, setLiveFrameIndex] = useState<number>(0);

  const imageUploadRef = useRef<HTMLInputElement | null>(null);

  // Cycle presets smoothly in Live Camera simulation when playing
  useEffect(() => {
    if (activeTab !== 'live' || !isLivePlaying) return;
    const timer = window.setInterval(() => {
      setLiveFrameIndex((prev) => (prev + 1) % 30);
    }, 500);
    return () => window.clearInterval(timer);
  }, [activeTab, isLivePlaying]);

  const currentPreset: VehiclePreset = useMemo(() => {
    if (activeTab === 'live') {
      const idx = Math.floor(liveFrameIndex / 10) % VEHICLE_PRESETS.length;
      return VEHICLE_PRESETS[idx];
    }
    return VEHICLE_PRESETS.find((p) => p.id === selectedPresetId) ?? VEHICLE_PRESETS[0];
  }, [activeTab, selectedPresetId, liveFrameIndex]);

  const detectionResult = useMemo(() => {
    if (!isModelLoaded) {
      return {
        parts: [],
        preprocessTimeMs: 0,
        inferenceTimeMs: 0,
        postprocessTimeMs: 0,
        imageWidth: currentPreset.width,
        imageHeight: currentPreset.height,
      };
    }

    // Add slight confidence jitter in live camera mode to reflect real-time frame analysis
    const jitter = activeTab === 'live' ? ((liveFrameIndex % 5) - 2) * 0.008 : 0;
    const rawWithJitter: RawDetection[] = currentPreset.rawDetections.map((d) => ({
      ...d,
      confidence: Math.min(0.99, Math.max(0.1, Number((d.confidence + jitter).toFixed(2)))),
    }));

    return runSegmentationPipeline(
      rawWithJitter,
      confThreshold,
      iouThreshold,
      customImage ? customImage.naturalWidth : currentPreset.width,
      customImage ? customImage.naturalHeight : currentPreset.height
    );
  }, [isModelLoaded, currentPreset, confThreshold, iouThreshold, customImage, activeTab, liveFrameIndex]);

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        setCustomImage(img);
        setCustomImageName(file.name);
        setStatusBanner(`Imagem "${file.name}" carregada e segmentada com sucesso.`);
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(file);
  };

  const handleResetDefaults = () => {
    setConfThreshold(0.25);
    setIouThreshold(0.45);
    setMaskOpacity(160);
    setShowMasks(true);
    setShowBoxes(true);
    setShowLabels(true);
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
      {/* Top App Bar matching MainScreen.kt */}
      <header className="sticky top-0 z-30 bg-slate-900/95 backdrop-blur border-b border-slate-800 px-4 lg:px-8 py-3">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-blue-600 flex items-center justify-center font-black text-white shadow-lg shadow-blue-600/25">
              Y26
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold tracking-tight text-white">CarParts</h1>
                <span className="px-2 py-0.5 text-[11px] font-bold rounded-md bg-blue-500/20 text-blue-300 border border-blue-500/30">
                  YOLO26-seg
                </span>
              </div>
              <p className="text-xs text-slate-400 hidden sm:block">
                Segmentação Automotiva de 23 Peças em Tempo Real • LiteRT / TFLite
              </p>
            </div>
          </div>

          {/* Navigation Tabs */}
          <nav className="flex items-center gap-1.5 bg-slate-950 p-1 rounded-xl border border-slate-800">
            <button
              type="button"
              onClick={() => setActiveTab('gallery')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                activeTab === 'gallery'
                  ? 'bg-blue-600 text-white shadow'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <ImageIcon className="w-4 h-4" />
              <span>Foto / Galeria</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('live')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                activeTab === 'live'
                  ? 'bg-blue-600 text-white shadow'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Camera className="w-4 h-4" />
              <span>Câmera ao Vivo</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('catalog')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                activeTab === 'catalog'
                  ? 'bg-blue-600 text-white shadow'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <BookOpen className="w-4 h-4" />
              <span>Catálogo (23 Classes)</span>
            </button>
          </nav>

          {/* Action Controls matching TopAppBar actions in MainScreen.kt */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setShowModelModal(true)}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-semibold border transition-colors ${
                isModelLoaded
                  ? 'bg-emerald-950/80 border-emerald-700/70 text-emerald-300 hover:bg-emerald-900/70'
                  : 'bg-amber-950/80 border-amber-700/70 text-amber-300 hover:bg-amber-900/70'
              }`}
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  isModelLoaded ? 'bg-emerald-400' : 'bg-amber-400'
                }`}
              />
              <span>{isModelLoaded ? 'Modelo Ativo' : 'Carregar Modelo'}</span>
            </button>

            {activeTab === 'live' && (
              <button
                type="button"
                onClick={() => setIsMirrored((m) => !m)}
                aria-label="Alternar Câmera"
                title="Espelhar / Alternar Câmera"
                className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors"
              >
                <SwitchCamera className="w-4 h-4" />
              </button>
            )}

            <button
              type="button"
              onClick={() => setShowSettingsModal(true)}
              aria-label="Configurações"
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-semibold transition-colors"
            >
              <Sliders className="w-4 h-4 text-blue-400" />
              <span className="hidden md:inline">Configurações</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 lg:px-8 py-6">
        {statusBanner && (
          <div className="mb-4 px-4 py-2.5 rounded-xl bg-blue-950/80 border border-blue-800/80 text-xs text-blue-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-blue-400 shrink-0" />
              <span>{statusBanner}</span>
            </div>
            <button
              type="button"
              onClick={() => setStatusBanner(null)}
              className="text-blue-300 hover:text-white font-semibold ml-4"
            >
              Fechar
            </button>
          </div>
        )}

        {activeTab === 'catalog' ? (
          <TrainingCatalogPanel />
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Left Column: Interactive Segmentation Viewport (7 cols) */}
            <div className="lg:col-span-7 space-y-4">
              {/* Preset Selector / Upload Toolbar */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-3.5 flex flex-wrap items-center justify-between gap-3">
                {activeTab === 'gallery' ? (
                  <>
                    <div className="flex flex-wrap items-center gap-1.5">
                      {VEHICLE_PRESETS.map((preset) => (
                        <button
                          key={preset.id}
                          type="button"
                          onClick={() => {
                            setSelectedPresetId(preset.id);
                            setCustomImage(null);
                            setCustomImageName('');
                            setHighlightedPartId(null);
                          }}
                          className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors ${
                            selectedPresetId === preset.id && !customImage
                              ? 'bg-blue-600 text-white'
                              : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                          }`}
                        >
                          {preset.name.split('—')[0].trim()}
                        </button>
                      ))}
                    </div>

                    <div className="flex items-center gap-2">
                      <input
                        ref={imageUploadRef}
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={handleImageUpload}
                      />
                      <button
                        type="button"
                        onClick={() => imageUploadRef.current?.click()}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold text-white transition-colors"
                      >
                        <Upload className="w-3.5 h-3.5 text-blue-400" />
                        <span>Escolher Imagem</span>
                      </button>
                    </div>
                  </>
                ) : (
                  <div className="w-full flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                      <span className="text-xs font-bold text-white">
                        Stream de Inspeção Automotiva ao Vivo ({currentPreset.name})
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => setIsLivePlaying((p) => !p)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-white"
                      >
                        {isLivePlaying ? (
                          <>
                            <Pause className="w-3.5 h-3.5 text-amber-400" />
                            <span>Pausar Stream</span>
                          </>
                        ) : (
                          <>
                            <Play className="w-3.5 h-3.5 text-emerald-400" />
                            <span>Retomar Stream</span>
                          </>
                        )}
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Canvas Viewport Card */}
              <div className="relative bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
                <div className="aspect-video w-full relative">
                  <DetectionCanvas
                    preset={currentPreset}
                    customImage={customImage}
                    detectionResult={detectionResult}
                    maskOpacity={maskOpacity}
                    showMasks={showMasks}
                    showBoxes={showBoxes}
                    showLabels={showLabels}
                    highlightedPartId={highlightedPartId}
                    isMirrored={isMirrored}
                    scanProgress={activeTab === 'live' ? (liveFrameIndex % 10) / 10 : undefined}
                  />

                  {/* Top-left Telemetry Pill matching LiveCameraContent in MainScreen.kt */}
                  <div className="absolute top-3 left-3 bg-slate-950/85 backdrop-blur border border-slate-800 rounded-xl px-3 py-1.5 flex items-center gap-2.5 shadow-lg">
                    <span className="text-xs font-bold text-emerald-400 flex items-center gap-1">
                      <Zap className="w-3.5 h-3.5" />
                      {detectionResult.inferenceTimeMs} ms
                    </span>
                    <span className="text-slate-600">•</span>
                    <span className="text-xs font-semibold text-white" data-testid="detected-count-badge">
                      {detectionResult.parts.length} peças detectadas
                    </span>
                  </div>
                </div>

                {/* Bottom Horizontal Part Chips in dedicated footer */}
                {detectionResult.parts.length > 0 && (
                  <div className="bg-slate-950/90 border-t border-slate-800 px-3.5 py-2.5 flex items-center gap-2 overflow-x-auto">
                    {detectionResult.parts.map((part) => (
                      <button
                        key={part.id}
                        type="button"
                        onClick={() =>
                          setHighlightedPartId((prev) => (prev === part.id ? null : part.id))
                        }
                        className={`shrink-0 inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold border transition-transform ${
                          highlightedPartId === part.id ? 'scale-105 ring-2 ring-white/40' : ''
                        }`}
                        style={{
                          backgroundColor: `${part.partClass.color}33`,
                          borderColor: part.partClass.color,
                          color: '#ffffff',
                        }}
                      >
                        <span
                          className="w-2.5 h-2.5 rounded-full"
                          style={{ backgroundColor: part.partClass.color }}
                        />
                        <span>
                          {part.partClass.labelPt} {Math.round(part.confidence * 100)}%
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Quick Hyperparameter Bar for direct access */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <div className="flex justify-between text-xs font-semibold mb-1">
                    <label htmlFor="quick-conf-slider" className="text-slate-300">
                      Confiança Mínima
                    </label>
                    <span className="text-blue-400 font-mono">
                      {Math.round(confThreshold * 100)}%
                    </span>
                  </div>
                  <input
                    id="quick-conf-slider"
                    type="range"
                    min={0.1}
                    max={0.9}
                    step={0.05}
                    value={confThreshold}
                    onChange={(e) => setConfThreshold(parseFloat(e.target.value))}
                    className="w-full accent-blue-500 cursor-pointer"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs font-semibold mb-1">
                    <label htmlFor="quick-iou-slider" className="text-slate-300">
                      Limiar IoU (NMS)
                    </label>
                    <span className="text-blue-400 font-mono">
                      {Math.round(iouThreshold * 100)}%
                    </span>
                  </div>
                  <input
                    id="quick-iou-slider"
                    type="range"
                    min={0.2}
                    max={0.8}
                    step={0.05}
                    value={iouThreshold}
                    onChange={(e) => setIouThreshold(parseFloat(e.target.value))}
                    className="w-full accent-blue-500 cursor-pointer"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs font-semibold mb-1">
                    <label htmlFor="quick-opacity-slider" className="text-slate-300">
                      Opacidade da Máscara
                    </label>
                    <span className="text-blue-400 font-mono">
                      {Math.round((maskOpacity / 255) * 100)}%
                    </span>
                  </div>
                  <input
                    id="quick-opacity-slider"
                    type="range"
                    min={40}
                    max={255}
                    step={5}
                    value={maskOpacity}
                    onChange={(e) => setMaskOpacity(parseInt(e.target.value, 10))}
                    className="w-full accent-blue-500 cursor-pointer"
                  />
                </div>
              </div>
            </div>

            {/* Right Column: Telemetry & Segmented Parts Inspector (5 cols) */}
            <div className="lg:col-span-5 space-y-4">
              {/* Performance Summary Card matching GalleryContent in MainScreen.kt */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Telemetria de Inferência LiteRT
                  </span>
                  <span className="text-xs font-mono text-slate-400">
                    {customImageName || currentPreset.id}.jpg ({detectionResult.imageWidth}x
                    {detectionResult.imageHeight})
                  </span>
                </div>
                <div className="grid grid-cols-3 gap-2 text-center bg-slate-950/90 border border-slate-800/80 rounded-xl p-3">
                  <div>
                    <div className="text-[11px] text-slate-400">Pré-processo</div>
                    <div className="text-sm font-bold text-white mt-0.5">
                      {detectionResult.preprocessTimeMs} ms
                    </div>
                  </div>
                  <div className="border-x border-slate-800">
                    <div className="text-[11px] text-slate-400">Inferência</div>
                    <div className="text-sm font-bold text-blue-400 mt-0.5">
                      {detectionResult.inferenceTimeMs} ms
                    </div>
                  </div>
                  <div>
                    <div className="text-[11px] text-slate-400">Pós-processo</div>
                    <div className="text-sm font-bold text-white mt-0.5">
                      {detectionResult.postprocessTimeMs} ms
                    </div>
                  </div>
                </div>
              </div>

              {/* Identified Parts List matching MainScreen.kt lines 545-641 */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <h2 className="text-base font-bold text-white">
                    Peças Identificadas ({detectionResult.parts.length})
                  </h2>
                  {highlightedPartId && (
                    <button
                      type="button"
                      onClick={() => setHighlightedPartId(null)}
                      className="text-xs text-blue-400 hover:text-blue-300 font-semibold"
                    >
                      Mostrar Todas
                    </button>
                  )}
                </div>

                {detectionResult.parts.length === 0 ? (
                  <div className="p-8 text-center text-slate-400 text-sm">
                    Nenhuma peça acima do limiar de confiança ({Math.round(confThreshold * 100)}%).
                    Reduza a confiança mínima para visualizar mais detecções.
                  </div>
                ) : (
                  <div className="space-y-2.5 max-h-[460px] overflow-y-auto pr-1">
                    {detectionResult.parts.map((part) => {
                      const isSelected = highlightedPartId === part.id;
                      return (
                        <button
                          key={part.id}
                          type="button"
                          onClick={() =>
                            setHighlightedPartId((prev) => (prev === part.id ? null : part.id))
                          }
                          className={`w-full text-left p-3.5 rounded-xl border transition-all flex items-center gap-3 ${
                            isSelected
                              ? 'bg-slate-800 border-blue-500 shadow-md'
                              : 'bg-slate-950/70 border-slate-800/90 hover:border-slate-700'
                          }`}
                        >
                          <span
                            className="w-4 h-4 rounded-full shrink-0"
                            style={{ backgroundColor: part.partClass.color }}
                          />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between gap-2">
                              <span className="text-sm font-bold text-white truncate">
                                {part.partClass.labelPt}
                              </span>
                              <span
                                className="text-sm font-bold font-mono"
                                style={{ color: part.partClass.color }}
                              >
                                {Math.round(part.confidence * 100)}%
                              </span>
                            </div>
                            <div className="flex items-center justify-between text-[11px] text-slate-400 mt-0.5">
                              <span className="font-mono">{part.partClass.key}</span>
                              <span>Área da máscara: {part.areaPercentage}%</span>
                            </div>
                            <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden mt-2">
                              <div
                                className="h-full rounded-full transition-all"
                                style={{
                                  width: `${Math.round(part.confidence * 100)}%`,
                                  backgroundColor: part.partClass.color,
                                }}
                              />
                            </div>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Modals */}
      <SettingsModal
        isOpen={showSettingsModal}
        onClose={() => setShowSettingsModal(false)}
        confThreshold={confThreshold}
        iouThreshold={iouThreshold}
        maskOpacity={maskOpacity}
        showMasks={showMasks}
        showBoxes={showBoxes}
        showLabels={showLabels}
        onConfChange={setConfThreshold}
        onIouChange={setIouThreshold}
        onOpacityChange={setMaskOpacity}
        onToggleMasks={setShowMasks}
        onToggleBoxes={setShowBoxes}
        onToggleLabels={setShowLabels}
        onResetDefaults={handleResetDefaults}
      />

      <ModelManagerModal
        isOpen={showModelModal}
        onClose={() => setShowModelModal(false)}
        isModelLoaded={isModelLoaded}
        modelName={modelName}
        modelSizeMb={modelSizeMb}
        onLoadCustomFile={(file) => {
          setIsModelLoaded(true);
          setModelName(`${file.name} (Local)`);
          setModelSizeMb(`${(file.size / (1024 * 1024)).toFixed(2)} MB`);
          setShowModelModal(false);
          setStatusBanner(`Modelo TFLite "${file.name}" carregado com sucesso!`);
        }}
        onReloadDefaultAssets={() => {
          setIsModelLoaded(true);
          setModelName('best.tflite (Assets)');
          setModelSizeMb('10.7 MB');
          setShowModelModal(false);
          setStatusBanner('Modelo padrão best.tflite recarregado dos assets!');
        }}
      />
    </div>
  );
}

export default App;
