// Core offline high-frequency vocabulary for HeliBoard predictive engine
// Format: [word, unigram frequency score (1-255)]

export const BASE_VOCABULARY: [string, number][] = [
  // Top 100 highest frequency words
  ['the', 255], ['be', 254], ['to', 253], ['of', 252], ['and', 251], ['a', 250],
  ['in', 249], ['that', 248], ['have', 247], ['i', 246], ['it', 245], ['for', 244],
  ['not', 243], ['on', 242], ['with', 241], ['he', 240], ['as', 239], ['you', 238],
  ['do', 237], ['at', 236], ['this', 235], ['but', 234], ['his', 233], ['by', 232],
  ['from', 231], ['they', 230], ['we', 229], ['say', 228], ['her', 227], ['she', 226],
  ['or', 225], ['an', 224], ['will', 223], ['my', 222], ['one', 221], ['all', 220],
  ['would', 219], ['there', 218], ['their', 217], ['what', 216], ['so', 215], ['up', 214],
  ['out', 213], ['if', 212], ['about', 211], ['who', 210], ['get', 209], ['which', 208],
  ['go', 207], ['me', 206], ['when', 205], ['make', 204], ['can', 203], ['like', 202],
  ['time', 201], ['no', 200], ['just', 199], ['him', 198], ['know', 197], ['take', 196],
  ['people', 195], ['into', 194], ['year', 193], ['your', 192], ['good', 191], ['some', 190],
  ['could', 189], ['them', 188], ['see', 187], ['other', 186], ['than', 185], ['then', 184],
  ['now', 183], ['look', 182], ['only', 181], ['come', 180], ['its', 179], ['over', 178],
  ['think', 177], ['also', 176], ['back', 175], ['after', 174], ['use', 173], ['two', 172],
  ['how', 171], ['our', 170], ['work', 169], ['first', 168], ['well', 167], ['way', 166],
  ['even', 165], ['new', 164], ['want', 163], ['because', 162], ['any', 161], ['these', 160],
  ['give', 159], ['day', 158], ['most', 157], ['us', 156],

  // Common conversational & messaging words
  ['hello', 180], ['hey', 175], ['hi', 170], ['thanks', 178], ['thank', 182], ['please', 175],
  ['sorry', 165], ['welcome', 150], ['okay', 172], ['sure', 168], ['great', 174], ['awesome', 155],
  ['cool', 160], ['nice', 166], ['fine', 158], ['amazing', 145], ['wonderful', 135], ['perfect', 152],
  ['tomorrow', 160], ['today', 170], ['tonight', 155], ['yesterday', 140], ['morning', 162],
  ['afternoon', 135], ['evening', 138], ['night', 165], ['weekend', 148], ['week', 155], ['month', 145],
  ['meeting', 148], ['call', 160], ['message', 152], ['text', 154], ['email', 150], ['phone', 155],
  ['home', 168], ['office', 142], ['school', 148], ['workplace', 120], ['store', 135], ['market', 130],
  ['food', 155], ['dinner', 148], ['lunch', 145], ['breakfast', 138], ['coffee', 150], ['tea', 132],
  ['water', 160], ['drink', 140], ['pizza', 130], ['sandwich', 120], ['restaurant', 135],
  ['happy', 162], ['sad', 130], ['love', 172], ['loved', 140], ['loving', 135], ['hate', 125],
  ['enjoy', 142], ['excited', 140], ['bored', 120], ['tired', 140], ['busy', 145], ['free', 148],
  ['ready', 155], ['already', 150], ['soon', 158], ['later', 162], ['almost', 145], ['always', 152],
  ['never', 150], ['sometimes', 142], ['often', 138], ['rarely', 120], ['maybe', 156], ['probably', 148],
  ['definitely', 145], ['actually', 154], ['really', 168], ['very', 170], ['quite', 135], ['too', 160],
  ['enough', 142], ['more', 165], ['less', 140], ['much', 160], ['many', 158], ['lot', 155],
  ['little', 152], ['big', 156], ['small', 150], ['huge', 130], ['tiny', 120], ['fast', 142],
  ['slow', 130], ['quick', 145], ['easy', 148], ['hard', 150], ['difficult', 135], ['simple', 142],
  ['important', 152], ['interesting', 140], ['special', 138], ['different', 150], ['same', 155],
  ['right', 168], ['wrong', 145], ['true', 148], ['false', 130], ['real', 150], ['best', 160],
  ['better', 158], ['worse', 130], ['worst', 125], ['beautiful', 140], ['pretty', 148], ['ugly', 115],

  // Actions & Verbs
  ['going', 165], ['doing', 160], ['having', 155], ['making', 150], ['taking', 148], ['getting', 158],
  ['coming', 152], ['looking', 148], ['thinking', 150], ['knowing', 135], ['seeing', 140], ['trying', 145],
  ['using', 142], ['working', 150], ['calling', 138], ['waiting', 142], ['talking', 140], ['leaving', 136],
  ['send', 155], ['sent', 148], ['sending', 142], ['check', 158], ['checked', 140], ['checking', 145],
  ['tell', 155], ['told', 145], ['telling', 135], ['ask', 148], ['asked', 140], ['asking', 138],
  ['need', 165], ['needed', 140], ['needing', 125], ['feel', 152], ['felt', 138], ['feeling', 145],
  ['find', 155], ['found', 148], ['finding', 135], ['give', 155], ['gave', 140], ['given', 135],
  ['start', 148], ['started', 142], ['starting', 140], ['stop', 145], ['stopped', 135], ['stopping', 125],
  ['help', 158], ['helped', 135], ['helping', 138], ['hope', 152], ['hoped', 125], ['hoping', 135],
  ['remember', 145], ['forget', 135], ['forgot', 138], ['understand', 145], ['agree', 140], ['believe', 142],
  ['bring', 140], ['brought', 135], ['buy', 142], ['bought', 135], ['pay', 140], ['paid', 138],
  ['write', 145], ['wrote', 132], ['written', 130], ['writing', 138], ['read', 145], ['reading', 138],
  ['listen', 138], ['hear', 142], ['heard', 140], ['watch', 142], ['play', 145], ['playing', 138],
  ['sleep', 138], ['wake', 132], ['eat', 140], ['eating', 135], ['drive', 135], ['driving', 130],
  ['walk', 138], ['walking', 132], ['run', 138], ['running', 135], ['travel', 130], ['visit', 132],

  // Keyboard, Tech, Floris & HeliBoard specific domain words
  ['keyboard', 160], ['typing', 155], ['florisboard', 170], ['heliboard', 170], ['dictionary', 160],
  ['gesture', 145], ['glide', 140], ['swipe', 148], ['autocorrect', 150], ['suggestion', 152],
  ['privacy', 165], ['offline', 160], ['learning', 155], ['prediction', 150], ['theme', 148],
  ['spacebar', 145], ['backspace', 142], ['cursor', 140], ['clipboard', 145], ['android', 150],
  ['layout', 142], ['subkeys', 135], ['monet', 138], ['material', 145], ['settings', 150],
  ['open', 155], ['source', 145], ['device', 140], ['screen', 145], ['battery', 135],
  ['internet', 140], ['secure', 145], ['security', 145], ['private', 150], ['history', 140],
  ['custom', 142], ['shortcut', 138], ['symbols', 135], ['number', 148], ['emoji', 145],

  // Additional everyday vocabulary
  ['something', 158], ['anything', 150], ['nothing', 145], ['everything', 152],
  ['someone', 148], ['anyone', 142], ['everyone', 145], ['nobody', 130],
  ['somewhere', 135], ['anywhere', 132], ['everywhere', 130],
  ['place', 150], ['house', 148], ['city', 145], ['world', 152], ['country', 145],
  ['friend', 155], ['friends', 150], ['family', 152], ['brother', 135], ['sister', 135],
  ['mother', 140], ['father', 140], ['parents', 138], ['child', 138], ['children', 142],
  ['problem', 148], ['question', 150], ['answer', 145], ['idea', 152], ['reason', 142],
  ['system', 142], ['program', 138], ['number', 145], ['part', 145], ['case', 140],
  ['point', 148], ['fact', 138], ['group', 140], ['company', 142], ['business', 142],
  ['story', 138], ['life', 155], ['hand', 148], ['head', 142], ['eye', 145], ['eyes', 145],
  ['face', 140], ['side', 142], ['money', 145], ['door', 138], ['car', 145], ['book', 140],
  ['word', 145], ['words', 142], ['name', 152], ['room', 142], ['line', 138], ['end', 145],
  ['game', 140], ['music', 145], ['picture', 138], ['photo', 140], ['video', 145], ['sound', 138]
];

// HeliBoard bigram transition model: precedes word -> likely followers with weights
export const BASE_BIGRAMS: Record<string, [string, number][]> = {
  'how': [
    ['are', 120], ['is', 100], ['about', 95], ['do', 90], ['can', 80],
    ['was', 75], ['did', 70], ['much', 65], ['many', 60]
  ],
  'how are': [
    ['you', 150], ['things', 100], ['we', 80], ['they', 75]
  ],
  'i': [
    ['am', 130], ['have', 125], ['will', 120], ['think', 115], ['want', 110],
    ['don\'t', 105], ['can', 100], ['know', 95], ['need', 90], ['was', 85],
    ['love', 80], ['feel', 75], ['would', 70], ['hope', 65]
  ],
  'i am': [
    ['going', 120], ['so', 110], ['not', 105], ['ready', 100], ['doing', 90],
    ['at', 85], ['in', 80], ['happy', 75], ['sorry', 70]
  ],
  'thank': [
    ['you', 160], ['god', 80]
  ],
  'thank you': [
    ['so', 130], ['very', 120], ['for', 115], ['much', 110]
  ],
  'what': [
    ['is', 130], ['are', 120], ['do', 115], ['about', 105], ['can', 95],
    ['time', 90], ['did', 85], ['happened', 80]
  ],
  'where': [
    ['are', 130], ['is', 125], ['do', 100], ['did', 90], ['can', 80]
  ],
  'let': [
    ['me', 140], ['us', 120], ['them', 80], ['it', 75]
  ],
  'let me': [
    ['know', 160], ['see', 110], ['check', 100], ['think', 90]
  ],
  'see': [
    ['you', 150], ['it', 100], ['what', 85], ['if', 80]
  ],
  'see you': [
    ['tomorrow', 140], ['later', 135], ['soon', 130], ['there', 100], ['again', 90]
  ],
  'good': [
    ['morning', 140], ['night', 130], ['idea', 110], ['job', 105], ['day', 100], ['luck', 95]
  ],
  'can': [
    ['you', 140], ['i', 130], ['we', 110], ['it', 85], ['be', 80]
  ],
  'can you': [
    ['please', 120], ['help', 115], ['send', 110], ['do', 105], ['come', 95], ['call', 90]
  ],
  'on': [
    ['my', 130], ['the', 125], ['time', 100], ['your', 90], ['it', 85]
  ],
  'on my': [
    ['way', 160], ['phone', 90], ['own', 85], ['mind', 80]
  ],
  'in': [
    ['the', 140], ['a', 120], ['my', 100], ['this', 90], ['case', 80]
  ],
  'to': [
    ['the', 130], ['be', 120], ['do', 110], ['go', 105], ['see', 100], ['get', 95]
  ],
  'we': [
    ['are', 130], ['can', 120], ['have', 115], ['will', 110], ['should', 95], ['need', 90]
  ],
  'you': [
    ['are', 140], ['can', 120], ['have', 115], ['know', 105], ['will', 100], ['want', 95]
  ],
  'it': [
    ['is', 150], ['was', 125], ['will', 110], ['would', 100], ['can', 90], ['looks', 85]
  ],
  'that': [
    ['is', 140], ['was', 120], ['would', 105], ['sounds', 100], ['makes', 90]
  ],
  'florisboard': [
    ['and', 120], ['keyboard', 110], ['is', 105], ['features', 90]
  ],
  'heliboard': [
    ['dictionary', 130], ['offline', 110], ['privacy', 105], ['learning', 95]
  ]
};

// HeliBoard shortcut expansions
export const DEFAULT_SHORTCUTS: Record<string, string> = {
  'omw': 'On my way!',
  'idk': "I don't know",
  'brb': 'Be right back',
  'tbh': 'To be honest',
  'btw': 'By the way',
  'np': 'No problem',
  'ty': 'Thank you',
  'yw': "You're welcome",
  'imo': 'In my opinion',
  'imho': 'In my humble opinion',
  'afk': 'Away from keyboard',
  'fyi': 'For your information',
  'tldr': 'Too long; didn\'t read',
  'rn': 'right now',
  'atm': 'at the moment',
  'hbu': 'How about you?',
  'wbu': 'What about you?',
  'lmk': 'Let me know',
  'gtg': 'Got to go',
  'ttyl': 'Talk to you later'
};

// Offensive words filter for HeliBoard privacy / safety blocklist
export const OFFENSIVE_WORDS = new Set<string>([
  'damn', 'hell', 'crap', 'ass', 'bitch', 'bastard', 'fuck', 'shit'
]);

// Physical keyboard layout key coordinate mapping for spatial typo tolerance
export const KEY_COORDINATES: Record<string, { x: number; y: number }> = {
  'q': { x: 0, y: 0 }, 'w': { x: 1, y: 0 }, 'e': { x: 2, y: 0 }, 'r': { x: 3, y: 0 },
  't': { x: 4, y: 0 }, 'y': { x: 5, y: 0 }, 'u': { x: 6, y: 0 }, 'i': { x: 7, y: 0 },
  'o': { x: 8, y: 0 }, 'p': { x: 9, y: 0 },
  'a': { x: 0.5, y: 1 }, 's': { x: 1.5, y: 1 }, 'd': { x: 2.5, y: 1 }, 'f': { x: 3.5, y: 1 },
  'g': { x: 4.5, y: 1 }, 'h': { x: 5.5, y: 1 }, 'j': { x: 6.5, y: 1 }, 'k': { x: 7.5, y: 1 },
  'l': { x: 8.5, y: 1 },
  'z': { x: 1.5, y: 2 }, 'x': { x: 2.5, y: 2 }, 'c': { x: 3.5, y: 2 }, 'v': { x: 4.5, y: 2 },
  'b': { x: 5.5, y: 2 }, 'n': { x: 6.5, y: 2 }, 'm': { x: 7.5, y: 2 }
};
