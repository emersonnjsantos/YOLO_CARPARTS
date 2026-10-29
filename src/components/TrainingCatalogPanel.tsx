import React, { useState } from 'react';
import { CAR_PARTS_CATALOG, CarPartClass } from '../ml/yoloEngine';
import { Layers, Award, Terminal, Search } from 'lucide-react';

export const TrainingCatalogPanel: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');

  const filteredClasses = CAR_PARTS_CATALOG.filter((item) => {
    const matchesCategory = selectedCategory === 'all' || item.category === selectedCategory;
    const matchesSearch =
      item.labelPt.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.key.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="space-y-6">
      {/* Summary Header Cards from yolo_carparts.ipynb */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <div className="text-xs text-slate-400">Arquitetura & Pesos</div>
          <div className="text-lg font-bold text-white mt-1">YOLO26n-seg (LiteRT)</div>
          <div className="text-xs text-emerald-400 mt-1">309 camadas • 3.06M parâmetros</div>
        </div>
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <div className="text-xs text-slate-400">Dataset Carparts-Seg</div>
          <div className="text-lg font-bold text-white mt-1">3.156 Train / 401 Val</div>
          <div className="text-xs text-blue-400 mt-1">50 Épocas • imgsz 640x640</div>
        </div>
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <div className="text-xs text-slate-400">Desempenho LiteRT (T4/Mobile)</div>
          <div className="text-lg font-bold text-white mt-1">15.1 ms Inferência</div>
          <div className="text-xs text-slate-400 mt-1">1.1 ms pré • 10.3 ms pós-processo</div>
        </div>
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <div className="text-xs text-slate-400">Exportação Android</div>
          <div className="text-lg font-bold text-white mt-1">best.tflite (10.7 MB)</div>
          <div className="text-xs text-purple-400 mt-1">Compatível com GPU Delegate</div>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <Layers className="w-5 h-5 text-blue-400" />
            <div>
              <h2 className="text-base font-bold text-white">
                Catálogo Oficial das 23 Classes (CarPartsCatalog)
              </h2>
              <p className="text-xs text-slate-400">
                Mapeamento completo de IDs, cores RGB e precisão mAP50 por peça automotiva
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Buscar peça ou chave..."
                aria-label="Buscar peça automotiva"
                className="pl-9 pr-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
              />
            </div>

            {[
              { id: 'all', label: 'Todas (23)' },
              { id: 'front', label: 'Dianteira' },
              { id: 'rear', label: 'Traseira' },
              { id: 'side', label: 'Laterais' },
              { id: 'glass_wheel', label: 'Vidros & Rodas' },
            ].map((cat) => (
              <button
                key={cat.id}
                type="button"
                onClick={() => setSelectedCategory(cat.id)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors ${
                  selectedCategory === cat.id
                    ? 'bg-blue-600 text-white'
                    : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {filteredClasses.map((part: CarPartClass) => (
            <div
              key={part.id}
              className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800/90 flex items-center justify-between gap-3"
            >
              <div className="flex items-center gap-3 min-w-0">
                <span
                  className="w-4 h-4 rounded-full shrink-0 border border-white/20"
                  style={{ backgroundColor: part.color }}
                />
                <div className="min-w-0">
                  <div className="text-sm font-bold text-white truncate">
                    #{part.id} {part.labelPt}
                  </div>
                  <div className="text-xs font-mono text-slate-400 truncate">{part.key}</div>
                </div>
              </div>
              <div className="text-right shrink-0">
                <div className="text-xs font-bold text-emerald-400">
                  mAP50 {(part.valMap50 * 100).toFixed(1)}%
                </div>
                <div className="text-[11px] text-slate-500">{part.instances} amostras</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Colab Pipeline Reference */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3">
        <div className="flex items-center gap-2 text-white font-bold text-sm">
          <Terminal className="w-4 h-4 text-emerald-400" />
          Pipeline de Treinamento e Exportação (yolo_carparts.ipynb)
        </div>
        <pre className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-slate-300 overflow-x-auto">
{`from ultralytics import YOLO

# 1. Treinamento em 23 classes de peças automotivas (Carparts Segmentation)
model = YOLO("yolo26n-seg.pt")
results = model.train(data="carparts-seg.yaml", epochs=50, imgsz=640)

# 2. Exportação LiteRT (TensorFlow Lite) para o App Android / Web
model.export(format="tflite", imgsz=640) # -> /content/runs/segment/train-2/weights/best.tflite (10.7 MB)`}
        </pre>
      </div>
    </div>
  );
};
