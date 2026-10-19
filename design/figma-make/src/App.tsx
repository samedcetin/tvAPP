import { useEffect, useMemo, useRef, useState } from "react";
import {
  AlertCircle,
  Check,
  ChevronLeft,
  ChevronRight,
  Clock,
  CornerDownLeft,
  Delete,
  Download,
  GripVertical,
  Home,
  Info,
  List,
  Lock,
  Moon,
  Play,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Star,
  StarOff,
  Tv,
  WifiOff,
  X,
} from "lucide-react";
import { categories, channels, getChannels, searchChannels, type Channel } from "./data/channels";
import { getStreamUrl, hasVerifiedStream } from "./data/streams";

type FocusableProps = {
  id: string;
  focused: string;
  onFocus: (id: string) => void;
  onPress?: () => void;
};

const galleryRoutes = [
  ["Açılış", "acilis"],
  ["Kumanda rehberi", "kumanda-rehberi"],
  ["Ana ekran", "ana"],
  ["Favori düzenleme", "ana?mod=favori"],
  ["Oynatıcı", "oynatici/trt1"],
  ["Bilgi bandı", "oynatici/trt1?katman=bant"],
  ["Kanal listesi", "oynatici/trt1?katman=liste"],
  ["Kanal seçenekleri", "oynatici/kanald?katman=secenekler"],
  ["Yayın açılamadı", "oynatici/startv?katman=hata"],
  ["Program yayınlanmıyor", "oynatici/trt1?katman=program-yok"],
  ["Uyku zamanlayıcı", "oynatici/trt1?katman=uyku"],
  ["Hâlâ izliyor musunuz?", "oynatici/trt1?katman=hala-izliyor"],
  ["Ayarlar", "ayarlar"],
  ["Yazı boyutu", "ayarlar?pencere=yazi-boyutu"],
  ["Hakkında", "hakkinda"],
  ["Tanı ekranı", "tani"],
  ["İnternet yok", "internet-yok"],
  ["Ebeveyn kilidi", "pin"],
  ["Kanal arama", "arama"],
  ["Program rehberi", "program-rehberi"],
  ["Favori sıralama", "favori-siralama"],
  ["Bileşen vitrini", "vitrin"],
] as const;

function go(route: string) {
  window.location.hash = route;
}

function setTextScale(value: number) {
  const scale = String(value);
  document.documentElement.style.setProperty("--ts", scale);
  document.documentElement.dataset.textScale = scale;
  localStorage.setItem("aileTv.yazıBoyutu", scale);
}

function getInitialFocus(route: string) {
  if (route === "kumanda-rehberi") return "guide-start";
  if (route.includes("katman=liste")) return `list-${route.split("/")[1]?.split("?")[0] || "trt1"}`;
  if (route.includes("katman=secenekler")) return "toggle-fav";
  if (route.includes("katman=hata")) {
    const channelId = route.split("/")[1]?.split("?")[0] || "";
    return hasVerifiedStream(channelId) ? "retry" : "channel-list";
  }
  if (route.includes("katman=program-yok")) return "other-channel";
  if (route.includes("katman=uyku")) return "sleep-0";
  if (route.includes("katman=hala-izliyor")) return "continue";
  if (route.includes("pencere=")) {
    const type = new URLSearchParams(route.split("?")[1] || "").get("pencere");
    if (type === "yazi-boyutu") {
      const scale = Number(localStorage.getItem("aileTv.yazıBoyutu") || "1");
      return `option-${[1, 1.25, 1.5].indexOf(scale)}`;
    }
    if (type === "kanal-yonu") return `option-${localStorage.getItem("aileTv.kanalYönü") === "onceki" ? 1 : 0}`;
    if (type === "bant-suresi") {
      const duration = Number(localStorage.getItem("aileTv.bantSüresi") || "5");
      return `option-${[3, 5, 10].indexOf(duration)}`;
    }
    return "option-0";
  }
  if (route === "ayarlar") return "setting-0";
  if (route === "pin") return "pin-0";
  if (route === "arama") return "key-0";
  if (route === "hakkinda") return "version";
  if (route === "internet-yok") return "retry-net";
  if (route === "vitrin") return "show-0";
  if (route.includes("mod=favori")) return "card-trt1";
  return "resume";
}

function LogoMark({ size = "normal" }: { size?: "normal" | "large" }) {
  return (
    <span className={`logo-mark ${size === "large" ? "logo-large" : ""}`}>
      <Tv />
    </span>
  );
}

function Button({
  id,
  focused,
  onFocus,
  onPress,
  children,
  icon: Icon,
  wide = false,
}: FocusableProps & { children: React.ReactNode; icon?: typeof Star; wide?: boolean }) {
  return (
    <button
      data-focus-id={id}
      className={`btn ${wide ? "btn-wide" : ""} ${focused === id ? "is-focused" : ""}`}
      onClick={() => {
        onFocus(id);
        onPress?.();
      }}
    >
      {Icon && <Icon />}
      {children}
    </button>
  );
}

function KeyHint({ keyName, children }: { keyName: string; children: React.ReactNode }) {
  return (
    <span className="key-hint">
      <span className="key-badge">{keyName === "back" ? <CornerDownLeft /> : keyName}</span>
      <span>{children}</span>
    </span>
  );
}

function Spinner({ small = false }: { small?: boolean }) {
  return <span className={`spinner ${small ? "spinner-small" : ""}`} />;
}

function ChannelLogo({ channel, large = false }: { channel: Channel; large?: boolean }) {
  return <span className={`channel-logo ${large ? "channel-logo-large" : ""}`}>{channel.name}</span>;
}

function ChannelCard({
  channel,
  id,
  focused,
  onFocus,
  onPress,
  favorite,
  editing,
}: FocusableProps & { channel: Channel; favorite: boolean; editing?: boolean }) {
  return (
    <button
      data-focus-id={id}
      className={`channel-card ${focused === id ? "is-focused" : ""}`}
      onClick={() => {
        onFocus(id);
        onPress?.();
      }}
    >
      <span className="card-art">
        <strong>{channel.name}</strong>
        {favorite && (
          <span className={`star-badge ${editing ? "edit-star" : ""}`}>
            <Star fill="currentColor" />
          </span>
        )}
        {editing && !favorite && (
          <span className="star-badge edit-star empty">
            <Star />
          </span>
        )}
      </span>
      <span className="card-label">
        <span>{channel.number}</span>
        <strong>{channel.name}</strong>
      </span>
    </button>
  );
}

function Toast({ message }: { message: string }) {
  return (
    <div className="toast">
      <Check />
      {message}
    </div>
  );
}

function Dialog({
  icon: Icon,
  title,
  description,
  children,
  wide = false,
  onClose,
}: {
  icon?: typeof Star;
  title: string;
  description?: string;
  children: React.ReactNode;
  wide?: boolean;
  onClose?: () => void;
}) {
  return (
    <div className="scrim" onClick={(event) => {
      if (event.target === event.currentTarget) onClose?.();
    }}>
      <section className={`dialog ${wide ? "dialog-wide" : ""}`} onClick={(event) => event.stopPropagation()}>
        {Icon && <Icon className="dialog-icon" />}
        <h2>{title}</h2>
        {description && <p>{description}</p>}
        <div className="dialog-actions">{children}</div>
      </section>
    </div>
  );
}

function Splash() {
  useEffect(() => {
    const timer = window.setTimeout(() => {
      go(localStorage.getItem("aileTv.rehberGörüldü") ? "ana" : "kumanda-rehberi");
    }, 1200);
    return () => clearTimeout(timer);
  }, []);
  return (
    <main className="center-screen">
      <LogoMark size="large" />
      <h1>Aile TV</h1>
      <div className="splash-loading">
        <Spinner small />
        <span>Kanallar hazırlanıyor…</span>
      </div>
    </main>
  );
}

function RemoteGuide({ focused, setFocused }: { focused: string; setFocused: (id: string) => void }) {
  const finish = () => {
    localStorage.setItem("aileTv.rehberGörüldü", "true");
    go("ana");
  };
  return (
    <main className="guide-screen">
      <div className="remote">
        <div className="remote-ring">
          <span>▲</span><span>◀</span><b>OK</b><span>▶</span><span>▼</span>
        </div>
        <div className="remote-small"><CornerDownLeft /><Home /></div>
        {[1, 2, 3, 4].map((item) => <i key={item} />)}
      </div>
      <section className="guide-copy">
        <h1>Kumandada bu tuşlar yeter</h1>
        <p>Aile TV'yi yalnızca yön tuşları, OK ve Geri ile kullanırsınız.</p>
        {[
          ["▲▼", "Kanal değiştir", "Yayın izlerken önceki veya sonraki kanala geçer."],
          ["OK", "Seç ve kanal listesi", "Seçtiğinizi açar. Yayın sırasında kanal listesini gösterir."],
          ["◀▶", "Menüde gezin", "Seçenekler arasında sağa ve sola gider."],
          ["back", "Geri", "Bir önceki ekrana döner. Arka arkaya basınca ana ekrana çıkarsınız."],
        ].map(([key, title, copy]) => (
          <div className="guide-row" key={title}>
            <span className="guide-key">{key === "back" ? <CornerDownLeft /> : key}</span>
            <span><strong>{title}</strong><small>{copy}</small></span>
          </div>
        ))}
        <Button id="guide-start" focused={focused} onFocus={setFocused} onPress={finish}>Anladım, başla</Button>
      </section>
    </main>
  );
}

function HomeScreen({
  focused,
  setFocused,
  favorites,
  toggleFavorite,
  toast,
  setToast,
}: {
  focused: string;
  setFocused: (id: string) => void;
  favorites: string[];
  toggleFavorite: (id: string) => boolean;
  toast: string;
  setToast: (message: string) => void;
}) {
  const params = new URLSearchParams(window.location.hash.split("?")[1] || "");
  const editing = params.get("mod") === "favori";
  const [filter, setFilter] = useState(localStorage.getItem("aileTv.aktifFiltre") || "tumu");
  const visible = getChannels(filter, favorites);
  const last = channels.find((item) => item.id === (localStorage.getItem("aileTv.sonKanal") || "trt1")) || channels[0];
  const play = (channel: Channel) => {
    localStorage.setItem("aileTv.sonKanal", channel.id);
    go(`oynatici/${channel.id}?katman=aciliyor`);
  };
  const setActiveFilter = (id: string) => {
    setFilter(id);
    localStorage.setItem("aileTv.aktifFiltre", id);
    setFocused(`chip-${id}`);
  };
  return (
    <main className={`home-screen ${editing ? "editing" : ""}`}>
      <header className="topbar">
        <div className="brand">
          {editing ? <Star /> : <LogoMark />}
          <h2>{editing ? "Favorileri düzenle" : "Aile TV"}</h2>
        </div>
        {editing ? (
          <>
            <div className="top-hints"><KeyHint keyName="OK">★ ekle veya çıkar</KeyHint><KeyHint keyName="back">Bitti</KeyHint></div>
            <Button id="done" focused={focused} onFocus={setFocused} onPress={() => go("ana")}>Bitti</Button>
          </>
        ) : (
          <div className="top-actions">
            <Button id="search" focused={focused} onFocus={setFocused} onPress={() => go("arama")} icon={Search}>Ara</Button>
            <Button id="favorites" focused={focused} onFocus={setFocused} onPress={() => go("ana?mod=favori")} icon={Star}>Favorileri düzenle</Button>
            <Button id="settings" focused={focused} onFocus={setFocused} onPress={() => go("ayarlar")} icon={SlidersHorizontal}>Ayarlar</Button>
            <ClockText />
          </div>
        )}
      </header>
      {!editing && (
        <button data-focus-id="resume" className={`resume-card ${focused === "resume" ? "is-focused" : ""}`} onClick={() => { setFocused("resume"); play(last); }}>
          <ChannelLogo channel={last} />
          <span><small>Kaldığın yerden</small><strong>{last.number} · {last.name}</strong></span>
          <KeyHint keyName="OK">İzlemeye devam et</KeyHint>
        </button>
      )}
      <div className="chip-row">
        {categories.map((category) => (
          <button
            key={category.id}
            data-focus-id={`chip-${category.id}`}
            className={`chip ${filter === category.id ? "is-selected" : ""} ${focused === `chip-${category.id}` ? "is-focused" : ""}`}
            onClick={() => setActiveFilter(category.id)}
          >
            {filter === category.id && <Check />}
            {category.id === "favoriler" && <Star fill="currentColor" />}
            {category.label}
          </button>
        ))}
      </div>
      {visible.length ? (
        <div className="channel-grid">
          {visible.map((channel) => (
            <ChannelCard
              key={channel.id}
              id={`card-${channel.id}`}
              focused={focused}
              onFocus={setFocused}
              onPress={() => {
                if (editing) {
                  const added = toggleFavorite(channel.id);
                  setToast(added ? "Favorilere eklendi" : "Favorilerden çıkarıldı");
                } else play(channel);
              }}
              channel={channel}
              favorite={favorites.includes(channel.id)}
              editing={editing}
            />
          ))}
        </div>
      ) : (
        <section className="empty-state">
          <Star />
          <h2>Henüz favori kanalınız yok.</h2>
          <p>Sık izlediğiniz kanalları favorilere ekleyin. Kanal değiştirirken yalnızca onlar arasında dolaşırsınız.</p>
          <div>
            <Button id="all-channels" focused={focused} onFocus={setFocused} onPress={() => setActiveFilter("tumu")}>Tüm kanallara git</Button>
            <Button id="edit-empty" focused={focused} onFocus={setFocused} onPress={() => go("ana?mod=favori")}>Favorileri düzenle</Button>
          </div>
        </section>
      )}
      {toast && <Toast message={toast} />}
      {params.get("katman") === "guncelleme" && (
        <Dialog icon={Download} title="Yeni sürüm hazır" description="Sürüm 1.1.0 · [Değişiklik notu]" onClose={() => go("ana")}>
          <p>Güncelleme sırasında televizyon bir onay penceresi gösterecek, orada “Yükle”ye basın.</p>
          <Button id="update-now" focused={focused} onFocus={setFocused} onPress={() => { go("ana"); setToast("Güncelleme hazırlandı"); }} wide>Şimdi güncelle</Button>
          <Button id="later" focused={focused} onFocus={setFocused} onPress={() => go("ana")} wide>Sonra</Button>
        </Dialog>
      )}
    </main>
  );
}

function ClockText() {
  const [time, setTime] = useState("");
  useEffect(() => {
    const update = () => setTime(new Intl.DateTimeFormat("tr-TR", { hour: "2-digit", minute: "2-digit" }).format(new Date()));
    update();
    const timer = setInterval(update, 1000);
    return () => clearInterval(timer);
  }, []);
  return <time>{time}</time>;
}

function StreamPlayer({
  channel,
  onPlaying,
  onError,
}: {
  channel: Channel;
  onPlaying: () => void;
  onError: () => void;
}) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const callbackRef = useRef({ onPlaying, onError });
  callbackRef.current = { onPlaying, onError };

  useEffect(() => {
    const video = videoRef.current;
    const url = getStreamUrl(channel.id);
    if (!video || !url) return;
    let hls: import("hls.js").default | null = null;
    let disposed = false;
    let started = false;
    let networkRetries = 0;
    let mediaRetries = 0;
    let nativeRetries = 0;
    let nativeRetryTimer: number | undefined;

    const handlePlaying = () => {
      if (started) return;
      started = true;
      callbackRef.current.onPlaying();
    };
    const handleError = () => callbackRef.current.onError();
    const startPlayback = async () => {
      try {
        await video.play();
      } catch {
        video.muted = true;
        try {
          await video.play();
        } catch {
          handleError();
        }
      }
    };

    const handleNativeError = () => {
      if (nativeRetries >= 2) {
        handleError();
        return;
      }
      nativeRetries += 1;
      nativeRetryTimer = window.setTimeout(() => {
        video.src = url;
        video.load();
        void startPlayback();
      }, nativeRetries * 900);
    };

    video.addEventListener("playing", handlePlaying);
    if (video.canPlayType("application/vnd.apple.mpegurl")) {
      video.addEventListener("error", handleNativeError);
      video.src = url;
      void startPlayback();
    } else {
      void import("hls.js").then(({ default: Hls }) => {
        if (disposed) return;
        if (!Hls.isSupported()) {
          handleError();
          return;
        }
        hls = new Hls({
          enableWorker: true,
          lowLatencyMode: false,
          liveSyncDurationCount: 5,
          maxBufferLength: 30,
          maxMaxBufferLength: 60,
          backBufferLength: 45,
        });
        hls.loadSource(url);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, () => void startPlayback());
        hls.on(Hls.Events.FRAG_LOADED, () => {
          networkRetries = 0;
          mediaRetries = 0;
        });
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (!data.fatal || !hls) return;
          if (data.type === Hls.ErrorTypes.NETWORK_ERROR && networkRetries < 2) {
            networkRetries += 1;
            hls.startLoad();
            return;
          }
          if (data.type === Hls.ErrorTypes.MEDIA_ERROR && mediaRetries < 2) {
            mediaRetries += 1;
            hls.recoverMediaError();
            return;
          }
          handleError();
        });
      });
    }

    return () => {
      disposed = true;
      video.removeEventListener("playing", handlePlaying);
      video.removeEventListener("error", handleNativeError);
      if (nativeRetryTimer) window.clearTimeout(nativeRetryTimer);
      hls?.destroy();
      video.removeAttribute("src");
      video.load();
    };
  }, [channel.id]);

  if (!getStreamUrl(channel.id)) return null;
  return <video ref={videoRef} className="live-video" autoPlay playsInline />;
}

function Player({
  route,
  focused,
  setFocused,
  favorites,
  toggleFavorite,
  setToast,
}: {
  route: string;
  focused: string;
  setFocused: (id: string) => void;
  favorites: string[];
  toggleFavorite: (id: string) => boolean;
  setToast: (message: string) => void;
}) {
  const id = route.split("/")[1]?.split("?")[0] || "trt1";
  const channel = channels.find((item) => item.id === id) || channels[0];
  const layer = new URLSearchParams(route.split("?")[1] || "").get("katman");
  const streamUrl = getStreamUrl(channel.id);
  const [isPlaying, setIsPlaying] = useState(false);
  const active = getChannels(localStorage.getItem("aileTv.aktifFiltre") || "tumu", favorites);
  const open = (next: Channel, nextLayer = "bant") => {
    localStorage.setItem("aileTv.sonKanal", next.id);
    go(`oynatici/${next.id}?katman=${nextLayer}`);
  };
  const next = (direction: number) => {
    const index = Math.max(0, active.findIndex((item) => item.id === channel.id));
    open(active[(index + direction + active.length) % active.length]);
  };
  useEffect(() => {
    setIsPlaying(false);
  }, [channel.id]);
  useEffect(() => {
    localStorage.setItem("aileTv.sonKanal", channel.id);
    if ((layer === "aciliyor" || layer === "harici" || layer === "bant") && !streamUrl) {
      const timer = setTimeout(() => go(`oynatici/${channel.id}?katman=hata`), 900);
      return () => clearTimeout(timer);
    }
  }, [channel.id, layer, streamUrl]);
  useEffect(() => {
    if (layer !== "bant" || !isPlaying) return;
    const duration = Number(localStorage.getItem("aileTv.bantSüresi") || "5");
    const timer = window.setTimeout(() => {
      go(`oynatici/${channel.id}`);
    }, duration * 1000);
    return () => window.clearTimeout(timer);
  }, [channel.id, isPlaying, layer]);
  return (
    <main className="player">
      {streamUrl ? (
        <StreamPlayer
          channel={channel}
          onPlaying={() => {
            setIsPlaying(true);
            if (layer === "aciliyor" || layer === "harici") go(`oynatici/${channel.id}?katman=bant`);
          }}
          onError={() => {
            if (layer !== "hata") go(`oynatici/${channel.id}?katman=hata`);
          }}
        />
      ) : (
        <div className="video-placeholder">[Bu kanal için doğrulanmış yayın bağlantısı bulunamadı · {channel.name}]</div>
      )}
      {(layer === "aciliyor" || layer === "harici") && (
        <section className="opening-state">
          <ChannelLogo channel={channel} large />
          <h2>{channel.number} · {channel.name}</h2>
          <span>{streamUrl ? "Canlı yayın açılıyor…" : "Yayın bağlantısı kontrol ediliyor…"}</span>
          <Spinner small />
        </section>
      )}
      {layer === "bant" && <InfoBanner channel={channel} favorite={favorites.includes(channel.id)} />}
      {layer === "liste" && (
        <aside className="side-panel">
          <h2>Kanallar</h2>
          <button className={`category-picker ${focused === "category-picker" ? "is-focused" : ""}`} onClick={() => setFocused("category-picker")}><ChevronLeft />Tümü · 55 kanal<ChevronRight /></button>
          <div className="channel-list">
            {channels.slice(Math.max(0, channel.number - 3), channel.number + 5).map((item) => (
              <button
                key={item.id}
                data-focus-id={`list-${item.id}`}
                className={`list-row ${item.id === channel.id ? "is-playing" : ""} ${focused === `list-${item.id}` ? "is-focused" : ""}`}
                data-channel-id={item.id}
                onClick={() => { setFocused(`list-${item.id}`); open(item); }}
              >
                <span>{item.number}</span><strong>{item.name}</strong>
                {item.id === channel.id && <><Play /><small>Şu an</small></>}
                {favorites.includes(item.id) && <Star className="list-star" fill="currentColor" />}
              </button>
            ))}
          </div>
          <div className="panel-hints"><KeyHint keyName="OK">İzle</KeyHint><KeyHint keyName="▶">Seçenekler</KeyHint><KeyHint keyName="back">Kapat</KeyHint></div>
        </aside>
      )}
      {layer === "secenekler" && (
        <Dialog title={`${channel.number} · ${channel.name}`} description={categories.find((item) => item.id === channel.category)?.label} onClose={() => go(`oynatici/${channel.id}?katman=liste`)}>
          <Button id="toggle-fav" focused={focused} onFocus={setFocused} icon={favorites.includes(channel.id) ? StarOff : Star} wide onPress={() => {
            const added = toggleFavorite(channel.id); setToast(added ? "Favorilere eklendi" : "Favorilerden çıkarıldı"); go(`oynatici/${channel.id}?katman=liste`);
          }}>{favorites.includes(channel.id) ? "Favorilerden çıkar" : "Favorilere ekle"}</Button>
          <Button id="reload" focused={focused} onFocus={setFocused} icon={RefreshCw} onPress={() => { setToast("Yayın yeniden yükleniyor"); go(`oynatici/${channel.id}?katman=aciliyor`); }} wide>Yayını yeniden yükle</Button>
          <Button id="close" focused={focused} onFocus={setFocused} icon={X} onPress={() => go(`oynatici/${channel.id}?katman=liste`)} wide>Kapat</Button>
        </Dialog>
      )}
      {layer === "hata" && (
        <Dialog
          icon={AlertCircle}
          title={`${channel.name} şu anda açılamıyor.`}
          description={hasVerifiedStream(channel.id) ? "Yayın bağlantısı yanıt vermedi. Biraz sonra tekrar deneyin veya başka bir kanal seçin." : "Bu kanal için doğrulanmış yayın bağlantısı bulunamadı. Başka bir kanal seçin."}
          onClose={() => go(hasVerifiedStream(channel.id) ? `oynatici/${channel.id}` : `oynatici/${channel.id}?katman=liste`)}
        >
          <div className="button-line">
            {hasVerifiedStream(channel.id) ? (
              <Button id="retry" focused={focused} onFocus={setFocused} icon={RefreshCw} onPress={() => go(`oynatici/${channel.id}?katman=aciliyor`)}>Tekrar dene</Button>
            ) : (
              <Button id="channel-list" focused={focused} onFocus={setFocused} icon={List} onPress={() => go(`oynatici/${channel.id}?katman=liste`)}>Kanal listesi</Button>
            )}
            <Button id="next" focused={focused} onFocus={setFocused} icon={ChevronRight} onPress={() => next(1)}>Sonraki kanal</Button>
            <Button id="home" focused={focused} onFocus={setFocused} icon={Home} onPress={() => go("ana")}>Ana ekran</Button>
          </div>
        </Dialog>
      )}
      {layer === "program-yok" && (
        <Dialog icon={Info} title="Bu program internetten yayınlanmıyor." description="Yayıncı bu programı internette göstermiyor. Program bitince yayın kendiliğinden devam eder." onClose={() => go(`oynatici/${channel.id}`)}>
          <Button id="other-channel" focused={focused} onFocus={setFocused} icon={List} onPress={() => go(`oynatici/${channel.id}?katman=liste`)} wide>Başka kanal seç</Button>
          <Button id="home" focused={focused} onFocus={setFocused} icon={Home} onPress={() => go("ana")} wide>Ana ekran</Button>
        </Dialog>
      )}
      {layer === "uyku" && (
        <Dialog icon={Moon} title="Uyku zamanlayıcı" description="Seçtiğiniz süre dolunca yayın durur." wide onClose={() => go(`oynatici/${channel.id}`)}>
          {["Kapalı", "30 dakika", "60 dakika", "90 dakika", "120 dakika"].map((option, index) => (
            <Button key={option} id={`sleep-${index}`} focused={focused} onFocus={setFocused} onPress={() => { setToast(index === 0 ? "Uyku zamanlayıcı kapatıldı" : `Uyku zamanlayıcı: ${option}`); go(`oynatici/${channel.id}?katman=bant`); }} wide>{option}</Button>
          ))}
        </Dialog>
      )}
      {layer === "hala-izliyor" && (
        <Dialog icon={Clock} title="Hâlâ izliyor musunuz?" description="Bir süredir kumandaya basılmadı. 60 saniye içinde yanıt vermezseniz yayın duracak." onClose={() => go(`oynatici/${channel.id}`)}>
          <strong className="countdown">0:58</strong>
          <Button id="continue" focused={focused} onFocus={setFocused} onPress={() => go(`oynatici/${channel.id}`)} wide>Evet, izlemeye devam</Button>
          <Button id="stop" focused={focused} onFocus={setFocused} onPress={() => go("ana")} wide>Yayını kapat</Button>
        </Dialog>
      )}
    </main>
  );
}

function InfoBanner({ channel, favorite }: { channel: Channel; favorite: boolean }) {
  return (
    <section className="info-banner">
      <div className="banner-main">
        <ChannelLogo channel={channel} />
        <div className="banner-title">
          <span><small>{channel.number}</small><strong>{channel.name}</strong></span>
          <div><em>{categories.find((item) => item.id === channel.category)?.label}</em><em className="live">CANLI</em></div>
        </div>
        <ClockText />{favorite && <Star className="banner-star" fill="currentColor" />}
      </div>
      <div className="banner-hints">
        <KeyHint keyName="▲▼">Kanal değiştir</KeyHint><KeyHint keyName="OK">Kanal listesi</KeyHint><KeyHint keyName="▶">Bilgi</KeyHint><KeyHint keyName="back">Ana ekran</KeyHint>
      </div>
    </section>
  );
}

function Settings({ focused, setFocused, setToast }: { focused: string; setFocused: (id: string) => void; setToast: (message: string) => void }) {
  const [autoStart, setAutoStart] = useState(() => localStorage.getItem("aileTv.açılıştaSonKanal") !== "false");
  const bannerDuration = Number(localStorage.getItem("aileTv.bantSüresi") || "5");
  const rows = [
    ["Yazı boyutu", "Normal", "ayarlar?pencere=yazi-boyutu"],
    ["Açılışta son kanalı başlat", autoStart ? "Açık" : "Kapalı", ""],
    ["Kanal değiştirme yönü", "▲ sonraki kanal", "ayarlar?pencere=kanal-yonu"],
    ["Bilgi bandı süresi", `${bannerDuration} saniye`, "ayarlar?pencere=bant-suresi"],
    ["Kanal listesini güncelle", "Son güncelleme: 18.10.2026", ""],
    ["Kumanda rehberini göster", "", "kumanda-rehberi"],
    ["Favori sıralama", "", "favori-siralama"],
    ["Hakkında", "Sürüm 1.0.0", "hakkinda"],
  ];
  const modal = new URLSearchParams(window.location.hash.split("?")[1] || "").get("pencere");
  return (
    <main className="settings-screen">
      <section className="settings-intro"><h1>Ayarlar</h1><p>Değişiklikler hemen uygulanır.</p></section>
      <div className="settings-list">
        {rows.map(([label, value, target], index) => (
          <button key={label} data-focus-id={`setting-${index}`} className={`settings-row ${focused === `setting-${index}` ? "is-focused" : ""}`} onClick={() => {
            setFocused(`setting-${index}`);
            if (index === 1) {
              const next = !autoStart;
              setAutoStart(next);
              localStorage.setItem("aileTv.açılıştaSonKanal", String(next));
              setToast(next ? "Açılışta son kanal açılacak" : "Ana ekran açılacak");
            } else if (index === 4) {
              setToast("Kanal listesi güncel");
            } else if (target) go(target);
          }}>
            <span><strong>{label}</strong>{value && <small>{value}</small>}</span>{index === 1 ? <span className={`switch ${autoStart ? "on" : ""}`}><i /></span> : <ChevronRight />}
          </button>
        ))}
      </div>
      {modal && (
        <SelectionDialog type={modal} focused={focused} setFocused={setFocused} />
      )}
    </main>
  );
}

function SelectionDialog({ type, focused, setFocused }: { type: string; focused: string; setFocused: (id: string) => void }) {
  const data: Record<string, [string, string[]]> = {
    "yazi-boyutu": ["Yazı boyutu", ["Normal", "Büyük", "Çok büyük"]],
    "kanal-yonu": ["Kanal değiştirme yönü", ["▲ sonraki kanal", "▲ önceki kanal"]],
    "bant-suresi": ["Bilgi bandı süresi", ["3 saniye", "5 saniye", "10 saniye"]],
  };
  const [title, options] = data[type] || data["yazi-boyutu"];
  const selectedIndex =
    type === "yazi-boyutu"
      ? [1, 1.25, 1.5].indexOf(Number(localStorage.getItem("aileTv.yazıBoyutu") || "1"))
      : type === "kanal-yonu"
        ? localStorage.getItem("aileTv.kanalYönü") === "onceki" ? 1 : 0
        : [3, 5, 10].indexOf(Number(localStorage.getItem("aileTv.bantSüresi") || "5"));
  return (
    <Dialog title={title} wide onClose={() => go("ayarlar")}>
      {options.map((option, index) => (
        <button key={option} data-focus-id={`option-${index}`} className={`radio-row ${index === selectedIndex ? "is-selected" : ""} ${focused === `option-${index}` ? "is-focused" : ""}`} onClick={() => {
          setFocused(`option-${index}`);
          if (type === "yazi-boyutu") {
            setTextScale([1, 1.25, 1.5][index]);
          } else if (type === "kanal-yonu") {
            localStorage.setItem("aileTv.kanalYönü", index === 0 ? "sonraki" : "onceki");
          } else if (type === "bant-suresi") {
            localStorage.setItem("aileTv.bantSüresi", String([3, 5, 10][index]));
          }
          go("ayarlar");
        }}>
          <span>{index === 0 && <Check />}</span>{option}
        </button>
      ))}
      {type === "yazi-boyutu" && <div className="type-preview"><strong>1 · TRT 1</strong><span>Kanal değiştir</span></div>}
    </Dialog>
  );
}

function SearchScreen({ focused, setFocused }: { focused: string; setFocused: (id: string) => void }) {
  const [query, setQuery] = useState("");
  const letters = [..."ABCÇDEFGĞH", ..."IİJKLMNOÖP", ..."RSŞTUÜVYZ"];
  const results = searchChannels(query);
  useEffect(() => {
    const handleBack = (event: KeyboardEvent) => {
      if (event.key !== "Escape" && event.key !== "Backspace") return;
      event.preventDefault();
      event.stopImmediatePropagation();
      if (query) setQuery((value) => value.slice(0, -1));
      else go("ana");
    };
    window.addEventListener("keydown", handleBack, true);
    return () => window.removeEventListener("keydown", handleBack, true);
  }, [query]);
  return (
    <main className="search-screen">
      <h1>Kanal ara</h1>
      <div className="search-box">
        <Search />
        <span className={query ? "" : "search-placeholder"}>{query || "Kanal adını yazın"}</span>
        <i />
      </div>
      <div className="search-content">
        <section className="keyboard" aria-label="Ekran klavyesi">
          {letters.map((letter, index) => <button key={letter} data-focus-id={`key-${index}`} className={focused === `key-${index}` ? "is-focused" : ""} onClick={() => { setFocused(`key-${index}`); setQuery(query + letter); }}>{letter}</button>)}
          <button data-focus-id="key-space" className="key-wide key-space" onClick={() => { setFocused("key-space"); setQuery(query + " "); }}>Boşluk</button>
          <button data-focus-id="key-delete" className="key-wide" onClick={() => { setFocused("key-delete"); setQuery(query.slice(0, -1)); }}><Delete /> Sil</button>
          <button data-focus-id="key-clear" className="key-wide" onClick={() => { setFocused("key-clear"); setQuery(""); }}>Temizle</button>
        </section>
        <section className="search-results">
          <header>
            <h2>Sonuçlar</h2>
            <span>{results.length} kanal</span>
          </header>
          <div className="results-list">
            {results.length ? results.map((channel, index) => (
              <button data-focus-id={`result-${index}`} className="list-row" key={channel.id} onClick={() => go(`oynatici/${channel.id}?katman=aciliyor`)}>
                <span>{channel.number}</span><strong>{channel.name}</strong><ChevronRight />
              </button>
            )) : <div className="no-results"><Search /><p>Bu adla bir kanal bulunamadı.</p><small>Farklı bir kanal adı deneyin.</small></div>}
          </div>
        </section>
      </div>
    </main>
  );
}

function PinScreen({ focused, setFocused }: { focused: string; setFocused: (id: string) => void }) {
  const [pin, setPin] = useState("");
  const keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "Sil", "0", "Vazgeç"];
  const press = (key: string) => {
    if (key === "Vazgeç") return go("ana");
    if (key === "Sil") return setPin(pin.slice(0, -1));
    const next = (pin + key).slice(0, 4);
    setPin(next);
    if (next === "1234") setTimeout(() => go("ayarlar"), 120);
    else if (next.length === 4) setTimeout(() => setPin(""), 350);
  };
  return (
    <main>
      <Dialog icon={Lock} title="PIN girin" description="Ayarları açmak için 4 haneli PIN'i girin." onClose={() => go("ana")}>
        <div className="pin-dots">{[0, 1, 2, 3].map((item) => <i className={pin.length > item ? "filled" : ""} key={item} />)}</div>
        <div className="pin-pad">{keys.map((key, index) => (
          <button key={key} data-focus-id={`pin-${index}`} className={focused === `pin-${index}` ? "is-focused" : ""} onClick={() => { setFocused(`pin-${index}`); press(key); }}>{key === "Sil" && <Delete />}{key}</button>
        ))}</div>
      </Dialog>
    </main>
  );
}

function About({ focused, setFocused }: { focused: string; setFocused: (id: string) => void }) {
  const [presses, setPresses] = useState(0);
  return (
    <main className="about-screen">
      <LogoMark size="large" /><h1>Aile TV</h1>
      {[["Sürüm", "1.0.0"], ["Kanal listesi", "18.10.2026 · 55 kanal"], ["Yayınlar", "Kanalların kendi resmi yayınları"]].map(([label, value], index) => (
        <button key={label} className={`about-row ${index === 0 && focused === "version" ? "is-focused" : ""}`} onClick={() => {
          if (index === 0) { setFocused("version"); const next = presses + 1; setPresses(next); if (next >= 5) go("tani"); }
        }}><span>{label}</span><strong>{value}</strong></button>
      ))}
      <p>Bu uygulama aile içi kullanım içindir. Kanal yayınları ve logoları ilgili yayıncılara aittir.</p>
    </main>
  );
}

function InternetOffline({ focused, setFocused, setToast }: { focused: string; setFocused: (id: string) => void; setToast: (message: string) => void }) {
  return (
    <main className="offline-screen">
      <WifiOff /><h1>İnternet bağlantısı yok</h1>
      <div>{["Modem açık mı?", "Televizyon kutusu Wi-Fi'a bağlı mı?", "Modemi kapatıp 10 saniye sonra tekrar açmayı deneyin."].map((item) => <p key={item}><Check />{item}</p>)}</div>
      <section><Button id="retry-net" focused={focused} onFocus={setFocused} icon={RefreshCw} onPress={() => go("ana")}>Tekrar dene</Button><Button id="network" focused={focused} onFocus={setFocused} icon={SlidersHorizontal} onPress={() => setToast("Televizyonun ağ ayarları açılır")}>Ağ ayarlarını aç</Button></section>
    </main>
  );
}

function GuideScreen() {
  return (
    <main className="program-screen">
      <h1>Program rehberi</h1>
      <div className="mini-chips"><span className="chip is-selected"><Check />Tümü</span><span className="chip">Genel</span><span className="chip">Haber</span><span className="chip">Spor</span></div>
      <div className="timeline"><span>20:00</span><span>20:30</span><span>21:00</span><span>21:30</span><span>22:00</span></div>
      <div className="program-grid">
        {channels.slice(0, 5).map((channel) => <div className="program-row" key={channel.id}><strong>{channel.number} · {channel.name}</strong><span className={channel.id === "trt1" ? "is-focused" : ""}>[Program adı]<small>20:00–21:30</small></span><span>[Program adı]<small>21:30–22:00</small></span></div>)}
      </div>
      <p className="program-note">Program bilgileri yayıncılardan alınır.</p>
    </main>
  );
}

function Showcase({ focused, setFocused }: { focused: string; setFocused: (id: string) => void }) {
  return (
    <main className="showcase">
      <header><LogoMark /><h1>Bileşen vitrini</h1></header>
      <div className="showcase-chips">{categories.map((c) => <span key={c.id} className={`chip ${c.id === "tumu" ? "is-selected" : ""}`}>{c.id === "tumu" && <Check />}{c.label}</span>)}</div>
      <div className="showcase-cards">{channels.slice(0, 5).map((channel, index) => <ChannelCard key={channel.id} channel={channel} id={`show-${index}`} focused={focused} onFocus={setFocused} favorite={index < 2} />)}</div>
      <div className="showcase-bottom"><Button id="normal" focused={focused} onFocus={setFocused}>Normal buton</Button><Button id="focus" focused="focus" onFocus={setFocused}>Odaklı buton</Button><span className="switch on"><i /></span><Spinner /><KeyHint keyName="OK">Seç</KeyHint></div>
    </main>
  );
}

function SimpleScreen({ type }: { type: string }) {
  if (type === "google-tv") return <main className="center-screen subtle"><h1>Google TV ana ekranı</h1><p>Aile TV'den çıktınız.</p><button className="btn is-focused" onClick={() => go("acilis")}>Aile TV'yi aç</button></main>;
  if (type === "tani") return <main className="diagnostic"><h1>Tanı ekranı <em>Geliştirici</em></h1><section><div><h2>Son basılan tuşlar</h2><p>20:45:12 · ArrowUp · KEYCODE_DPAD_UP (19)</p><p>20:45:14 · Enter · KEYCODE_DPAD_CENTER (23)</p></div><div><h2>Cihaz</h2><p>Android TV sürümü <strong>[11 / 14]</strong></p><p>İşlemci mimarisi <strong>[armeabi-v7a]</strong></p><p>Ekran <strong>1920×1080 · [50 / 60 Hz]</strong></p></div></section></main>;
  if (type === "favori-siralama") return <main className="sort-screen"><section><h1>Favori sıralama</h1><p>OK ile seçin, ▲▼ ile taşıyın, OK ile bırakın.</p><small>Kanal değiştirirken bu sırayla dolaşırsınız.</small></section><div>{channels.filter(c => c.defaultFavorite).map((c, i) => <div className={`list-row ${i === 1 ? "is-focused" : ""}`} key={c.id}><GripVertical /><strong>{c.number} · {c.name}</strong>{i === 1 && <em>Taşınıyor</em>}</div>)}</div></main>;
  return <main className="center-screen"><h1>Bu ekran sonraki adımda yapılacak</h1></main>;
}

function DeveloperPanel({ onClose, resetFavorites }: { onClose: () => void; resetFavorites: () => void }) {
  return (
    <aside className="developer-panel">
      <header><strong>Tasarım Paneli</strong><button onClick={onClose}><X /></button></header>
      <label>Yazı boyutu</label><div><button onClick={() => setTextScale(1)}>Normal</button><button onClick={() => setTextScale(1.25)}>Büyük</button><button onClick={() => setTextScale(1.5)}>Çok büyük</button></div>
      <label>Kart biçimi</label><div><button>16:9</button><button>1:1</button></div>
      <label>Filtre uygulama</label><div><button>Odaklanınca</button><button>OK'a basınca</button></div>
      <button>Yayın hatası simülasyonu</button><button>Program yayınlanmıyor</button><button>İnternet yok</button>
      <button onClick={resetFavorites}>Favorileri sıfırla</button><button onClick={() => localStorage.removeItem("aileTv.rehberGörüldü")}>Kumanda rehberini tekrar göster</button>
    </aside>
  );
}

function Gallery({ onClose }: { onClose: () => void }) {
  return (
    <div className="gallery-overlay"><header><h2>Ekran Galerisi</h2><button onClick={onClose}><X /></button></header><div>{galleryRoutes.map(([label, route]) => <button key={route} onClick={() => { go(route); onClose(); }}><span>{label}</span><small>#{route}</small><ChevronRight /></button>)}</div></div>
  );
}

export default function App() {
  const [route, setRoute] = useState(window.location.hash.slice(1) || "acilis");
  const [focused, setFocused] = useState(() => getInitialFocus(window.location.hash.slice(1) || "acilis"));
  const [gallery, setGallery] = useState(false);
  const [panel, setPanel] = useState(false);
  const [toast, setToastState] = useState("");
  const toastTimer = useRef<number | undefined>(undefined);
  const defaultFavorites = useMemo(() => channels.filter((c) => c.defaultFavorite).map((c) => c.id), []);
  const [favorites, setFavorites] = useState<string[]>(() => {
    try { return JSON.parse(localStorage.getItem("aileTv.favoriler") || "null") || defaultFavorites; } catch { return defaultFavorites; }
  });
  const setToast = (message: string) => {
    setToastState(message);
    clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToastState(""), 2000);
  };
  const toggleFavorite = (id: string) => {
    const added = !favorites.includes(id);
    const next = added ? [...favorites, id] : favorites.filter((item) => item !== id);
    setFavorites(next);
    localStorage.setItem("aileTv.favoriler", JSON.stringify(next));
    return added;
  };
  useEffect(() => {
    setTextScale(Number(localStorage.getItem("aileTv.yazıBoyutu") || "1"));
    const hash = () => {
      const next = window.location.hash.slice(1) || "acilis";
      setRoute(next);
      setFocused(getInitialFocus(next));
    };
    window.addEventListener("hashchange", hash);
    if (!window.location.hash) go("acilis");
    return () => window.removeEventListener("hashchange", hash);
  }, []);
  useEffect(() => {
    const key = (event: KeyboardEvent) => {
      if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Tab", "Enter", " ", "Backspace"].includes(event.key)) event.preventDefault();
      if ((event.key === "Enter" || event.key === " ") && event.repeat) return;
      if (event.key.toLowerCase() === "g") return setGallery((value) => !value);
      if (event.key.toLowerCase() === "t") return setPanel((value) => !value);
      if (event.key === "Escape" || event.key === "Backspace") {
        if (gallery) return setGallery(false);
        if (panel) return setPanel(false);
        if (route.includes("pencere=")) return go("ayarlar");
        if (route.includes("katman=secenekler")) {
          const id = route.split("/")[1]?.split("?")[0] || "trt1";
          return go(`oynatici/${id}?katman=liste`);
        }
        if (route.startsWith("oynatici/") && route.includes("?katman=")) return go(route.split("?")[0]);
        if (route.startsWith("ana?")) return go("ana");
        if (route.startsWith("oynatici")) return go("ana");
        if (route === "ana") return go("google-tv");
        if (route === "ayarlar") return go("ana");
        if (route === "hakkinda") return go("ayarlar");
        if (route === "tani") return go("hakkinda");
        if (route === "pin" || route === "arama" || route === "program-rehberi" || route === "favori-siralama") return go("ana");
        if (route === "internet-yok") return go("google-tv");
        return;
      }
      if (route.startsWith("oynatici/") && !["liste", "secenekler", "uyku", "hala-izliyor"].some((layer) => route.includes(`katman=${layer}`)) && (event.key === "ArrowUp" || event.key === "ArrowDown" || event.key === "PageUp" || event.key === "PageDown")) {
        const id = route.split("/")[1].split("?")[0];
        const current = channels.findIndex((c) => c.id === id);
        const upMovesNext = localStorage.getItem("aileTv.kanalYönü") !== "onceki";
        const isUp = event.key === "ArrowUp" || event.key === "PageUp";
        const direction = isUp === upMovesNext ? 1 : -1;
        const next = channels[(current + direction + channels.length) % channels.length];
        return go(`oynatici/${next.id}?katman=bant`);
      }
      if (route.includes("katman=liste") && event.key === "ArrowRight") {
        const activeRow = document.activeElement instanceof HTMLElement && document.activeElement.matches(".list-row")
          ? document.activeElement
          : document.querySelector<HTMLElement>(".channel-list .list-row.is-focused");
        const channelId = activeRow?.dataset.channelId;
        if (channelId) return go(`oynatici/${channelId}?katman=secenekler`);
      }
      if (route.startsWith("oynatici/") && (!route.includes("katman") || route.includes("katman=bant")) && (event.key === "Enter" || event.key === "ArrowLeft")) {
        const id = route.split("/")[1].split("?")[0]; return go(`oynatici/${id}?katman=liste`);
      }
      if (route.startsWith("oynatici/") && !route.includes("katman") && event.key === "ArrowRight") {
        const id = route.split("/")[1]; return go(`oynatici/${id}?katman=bant`);
      }
      if (route.startsWith("ana") && !document.querySelector(".scrim") && ["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(event.key)) {
        const active = document.activeElement instanceof HTMLButtonElement
          ? document.activeElement
          : document.querySelector<HTMLButtonElement>("button.is-focused");
        const topButtons = Array.from(document.querySelectorAll<HTMLButtonElement>(".topbar button"));
        const resume = document.querySelector<HTMLButtonElement>(".resume-card");
        const chips = Array.from(document.querySelectorAll<HTMLButtonElement>(".chip-row .chip"));
        const cards = Array.from(document.querySelectorAll<HTMLButtonElement>(".channel-grid .channel-card"));
        const selectedChip = document.querySelector<HTMLButtonElement>(".chip-row .chip.is-selected") || chips[0];
        const moveFocus = (target?: HTMLButtonElement | null) => {
          if (!target) return;
          const focusId = target.dataset.focusId;
          if (focusId) setFocused(focusId);
          target.focus({ preventScroll: true });
          target.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "nearest" });
        };

        if (active && topButtons.includes(active)) {
          const index = topButtons.indexOf(active);
          if (event.key === "ArrowLeft") moveFocus(topButtons[index - 1]);
          else if (event.key === "ArrowRight") moveFocus(topButtons[index + 1]);
          else if (event.key === "ArrowDown") moveFocus(resume || selectedChip);
          return;
        }
        if (active === resume) {
          if (event.key === "ArrowUp") moveFocus(topButtons.find((button) => button.dataset.focusId === "favorites") || topButtons[0]);
          else if (event.key === "ArrowDown") moveFocus(selectedChip);
          return;
        }
        if (active && chips.includes(active)) {
          const index = chips.indexOf(active);
          if (event.key === "ArrowLeft") moveFocus(chips[index - 1]);
          else if (event.key === "ArrowRight") moveFocus(chips[index + 1]);
          else if (event.key === "ArrowUp") moveFocus(resume || topButtons[0]);
          else if (event.key === "ArrowDown") moveFocus(cards[0]);
          return;
        }
        if (active && cards.includes(active)) {
          const index = cards.indexOf(active);
          const scale = document.documentElement.dataset.textScale || "1";
          const columns = scale === "1.5" ? 3 : scale === "1.25" ? 4 : 5;
          const column = index % columns;
          if (event.key === "ArrowLeft" && column > 0) moveFocus(cards[index - 1]);
          else if (event.key === "ArrowRight" && column < columns - 1 && cards[index + 1]) moveFocus(cards[index + 1]);
          else if (event.key === "ArrowUp") moveFocus(index < columns ? selectedChip : cards[index - columns]);
          else if (event.key === "ArrowDown") moveFocus(cards[index + columns]);
          return;
        }
      }
      const focusScope = document.querySelector(".scrim") || document.querySelector(".side-panel") || document;
      const focusables = Array.from(focusScope.querySelectorAll<HTMLElement>("button:not(:disabled)")).filter((item) => !item.closest(".developer-panel") && !item.closest(".gallery-overlay"));
      const currentIndex = Math.max(0, focusables.findIndex((item) => item === document.activeElement || item.classList.contains("is-focused")));
      const current = focusables[currentIndex];
      if (["ArrowRight", "ArrowDown", "ArrowLeft", "ArrowUp"].includes(event.key) && current) {
        const origin = current.getBoundingClientRect();
        const originX = origin.left + origin.width / 2;
        const originY = origin.top + origin.height / 2;
        const direction = event.key;
        const candidates = focusables
          .filter((item) => item !== current)
          .map((item) => {
            const rect = item.getBoundingClientRect();
            const x = rect.left + rect.width / 2;
            const y = rect.top + rect.height / 2;
            const dx = x - originX;
            const dy = y - originY;
            const valid =
              (direction === "ArrowRight" && dx > 4) ||
              (direction === "ArrowLeft" && dx < -4) ||
              (direction === "ArrowDown" && dy > 4) ||
              (direction === "ArrowUp" && dy < -4);
            const horizontal = direction === "ArrowRight" || direction === "ArrowLeft";
            const primary = horizontal ? Math.abs(dx) : Math.abs(dy);
            const secondary = horizontal ? Math.abs(dy) : Math.abs(dx);
            return { item, valid, score: primary + secondary * 2.4 };
          })
          .filter((candidate) => candidate.valid)
          .sort((a, b) => a.score - b.score);
        const next = candidates[0]?.item;
        const focusId = next?.dataset.focusId;
        if (focusId) setFocused(focusId);
        next?.focus({ preventScroll: true });
        next?.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "nearest" });
      } else if (event.key === "Enter" || event.key === " ") {
        const active = document.activeElement;
        if (active instanceof HTMLButtonElement) active.click();
        else current?.click();
      }
    };
    window.addEventListener("keydown", key);
    return () => window.removeEventListener("keydown", key);
  }, [route, gallery, panel]);

  let screen: React.ReactNode;
  if (route === "acilis") screen = <Splash />;
  else if (route === "kumanda-rehberi") screen = <RemoteGuide focused={focused} setFocused={setFocused} />;
  else if (route.startsWith("ana")) screen = <HomeScreen focused={focused} setFocused={setFocused} favorites={favorites} toggleFavorite={toggleFavorite} toast={toast} setToast={setToast} />;
  else if (route.startsWith("oynatici/")) screen = <Player route={route} focused={focused} setFocused={setFocused} favorites={favorites} toggleFavorite={toggleFavorite} setToast={setToast} />;
  else if (route.startsWith("ayarlar")) screen = <Settings focused={focused} setFocused={setFocused} setToast={setToast} />;
  else if (route === "arama") screen = <SearchScreen focused={focused} setFocused={setFocused} />;
  else if (route === "pin") screen = <PinScreen focused={focused} setFocused={setFocused} />;
  else if (route === "hakkinda") screen = <About focused={focused} setFocused={setFocused} />;
  else if (route === "internet-yok") screen = <InternetOffline focused={focused} setFocused={setFocused} setToast={setToast} />;
  else if (route === "program-rehberi") screen = <GuideScreen />;
  else if (route === "vitrin") screen = <Showcase focused={focused} setFocused={setFocused} />;
  else screen = <SimpleScreen type={route} />;
  return (
    <div className="viewport">
      <div id="tv-stage">{screen}{toast && !route.startsWith("ana") && <Toast message={toast} />}{panel && <DeveloperPanel onClose={() => setPanel(false)} resetFavorites={() => setFavorites(defaultFavorites)} />}{gallery && <Gallery onClose={() => setGallery(false)} />}</div>
    </div>
  );
}
