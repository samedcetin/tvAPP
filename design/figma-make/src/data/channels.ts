export type ChannelSource = "hls" | "youtube" | "web";
export type Channel = { id: string; number: number; name: string; category: string; source: ChannelSource; defaultFavorite: boolean };

export const categories = [
  {
    "id": "favoriler",
    "label": "Favoriler"
  },
  {
    "id": "tumu",
    "label": "Tümü"
  },
  {
    "id": "genel",
    "label": "Genel"
  },
  {
    "id": "haber",
    "label": "Haber"
  },
  {
    "id": "spor",
    "label": "Spor"
  },
  {
    "id": "cocuk",
    "label": "Çocuk"
  },
  {
    "id": "belgesel",
    "label": "Belgesel"
  },
  {
    "id": "muzik",
    "label": "Müzik"
  },
  {
    "id": "diger",
    "label": "Diğer"
  }
] as const;

export const channels: Channel[] = [
  {
    "id": "trt1",
    "number": 1,
    "name": "TRT 1",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": true
  },
  {
    "id": "atv",
    "number": 2,
    "name": "ATV",
    "category": "genel",
    "source": "web",
    "defaultFavorite": true
  },
  {
    "id": "kanald",
    "number": 3,
    "name": "Kanal D",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": true
  },
  {
    "id": "showtv",
    "number": 4,
    "name": "Show TV",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": true
  },
  {
    "id": "startv",
    "number": 5,
    "name": "Star TV",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": true
  },
  {
    "id": "now",
    "number": 6,
    "name": "NOW",
    "category": "genel",
    "source": "web",
    "defaultFavorite": false
  },
  {
    "id": "tv8",
    "number": 7,
    "name": "TV8",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": true
  },
  {
    "id": "kanal7",
    "number": 8,
    "name": "Kanal 7",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "teve2",
    "number": 9,
    "name": "teve2",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "beyaztv",
    "number": 10,
    "name": "Beyaz TV",
    "category": "genel",
    "source": "web",
    "defaultFavorite": false
  },
  {
    "id": "tv360",
    "number": 11,
    "name": "360",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trt2",
    "number": 12,
    "name": "TRT 2",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trthaber",
    "number": 13,
    "name": "TRT Haber",
    "category": "haber",
    "source": "hls",
    "defaultFavorite": true
  },
  {
    "id": "ntv",
    "number": 14,
    "name": "NTV",
    "category": "haber",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "cnnturk",
    "number": 15,
    "name": "CNN Türk",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "haberturk",
    "number": 16,
    "name": "Habertürk",
    "category": "haber",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "ahaber",
    "number": 17,
    "name": "A Haber",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "sozcutv",
    "number": 18,
    "name": "Sözcü TV",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "tv100",
    "number": 19,
    "name": "TV100",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "haberglobal",
    "number": 20,
    "name": "Haber Global",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "tgrthaber",
    "number": 21,
    "name": "TGRT Haber",
    "category": "haber",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "tv24",
    "number": 22,
    "name": "24 TV",
    "category": "haber",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "tvnet",
    "number": 23,
    "name": "TVNET",
    "category": "haber",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "halktv",
    "number": 24,
    "name": "Halk TV",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "ulketv",
    "number": 25,
    "name": "Ülke TV",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "akittv",
    "number": 26,
    "name": "Akit TV",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "benguturk",
    "number": 27,
    "name": "Bengütürk",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "bloomberght",
    "number": 28,
    "name": "Bloomberg HT",
    "category": "haber",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "apara",
    "number": 29,
    "name": "A Para",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "ekoturk",
    "number": 30,
    "name": "Ekotürk",
    "category": "haber",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "cnbce",
    "number": 31,
    "name": "CNBC-e",
    "category": "haber",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "trtspor",
    "number": 32,
    "name": "TRT Spor",
    "category": "spor",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtsporyildiz",
    "number": 33,
    "name": "TRT Spor Yıldız",
    "category": "spor",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "aspor",
    "number": 34,
    "name": "A Spor",
    "category": "spor",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "htspor",
    "number": 35,
    "name": "HT Spor",
    "category": "spor",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtcocuk",
    "number": 36,
    "name": "TRT Çocuk",
    "category": "cocuk",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtdiyanetcocuk",
    "number": 37,
    "name": "TRT Diyanet Çocuk",
    "category": "cocuk",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "minikacocuk",
    "number": 38,
    "name": "Minika Çocuk",
    "category": "cocuk",
    "source": "web",
    "defaultFavorite": false
  },
  {
    "id": "minikago",
    "number": 39,
    "name": "Minika GO",
    "category": "cocuk",
    "source": "web",
    "defaultFavorite": false
  },
  {
    "id": "trtbelgesel",
    "number": 40,
    "name": "TRT Belgesel",
    "category": "belgesel",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "dmax",
    "number": 41,
    "name": "DMAX",
    "category": "belgesel",
    "source": "web",
    "defaultFavorite": false
  },
  {
    "id": "tlc",
    "number": 42,
    "name": "TLC",
    "category": "belgesel",
    "source": "web",
    "defaultFavorite": false
  },
  {
    "id": "tgrtbelgesel",
    "number": 43,
    "name": "TGRT Belgesel",
    "category": "belgesel",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtmuzik",
    "number": 44,
    "name": "TRT Müzik",
    "category": "muzik",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "dreamturk",
    "number": 45,
    "name": "Dream Türk",
    "category": "muzik",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtgenc",
    "number": 46,
    "name": "TRT Genç",
    "category": "genel",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "diyanettv",
    "number": 47,
    "name": "Diyanet TV",
    "category": "diger",
    "source": "youtube",
    "defaultFavorite": false
  },
  {
    "id": "tbmmtv",
    "number": 48,
    "name": "TBMM TV",
    "category": "diger",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtturk",
    "number": 49,
    "name": "TRT Türk",
    "category": "diger",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtavaz",
    "number": 50,
    "name": "TRT Avaz",
    "category": "diger",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtkurdi",
    "number": 51,
    "name": "TRT Kurdî",
    "category": "diger",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtarabi",
    "number": 52,
    "name": "TRT Arabi",
    "category": "diger",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trtworld",
    "number": 53,
    "name": "TRT World",
    "category": "diger",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "trteba",
    "number": 54,
    "name": "TRT EBA",
    "category": "diger",
    "source": "hls",
    "defaultFavorite": false
  },
  {
    "id": "showturk",
    "number": 55,
    "name": "Show Türk",
    "category": "diger",
    "source": "hls",
    "defaultFavorite": false
  }
];

export function getChannels(filter: string, favorites: string[]) {
  if (filter === "favoriler") return channels.filter((channel) => favorites.includes(channel.id));
  if (filter === "tumu") return channels;
  return channels.filter((channel) => channel.category === filter);
}

export function searchChannels(query: string) {
  const normalized = query.toLocaleLowerCase("tr-TR");
  return channels.filter((channel) => channel.name.toLocaleLowerCase("tr-TR").includes(normalized));
}
