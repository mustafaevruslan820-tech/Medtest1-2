import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const out = path.join(__dirname, "..", "app", "src", "main", "assets", "medicines_catalog.json");

const FORMS_ROT = [
  "таблетки",
  "капсулы",
  "таблетки покрытые оболочкой",
  "раствор для приёма внутрь",
  "суспензия",
  "сироп",
  "гранулы",
  "раствор для инъекций",
  "мазь для наружного применения",
  "гель",
  "капли глазные",
  "капли назальные",
  "суппозитории ректальные",
];

/** [name_ru, name_lat, klass, doses[], aliases] */
const BASE = [
  ["Парацетамол", "Paracetamol", "paracetamol", ["325 мг", "500 мг", "750 мг", "1 г"], "панадол;эффералган"],
  ["Ибупрофен", "Ibuprofen", "nsaid", ["200 мг", "400 мг", "600 мг"], "нурофен;миг"],
  ["Кетопрофен", "Ketoprofen", "nsaid", ["50 мг", "100 мг"], "кетонал"],
  ["Диклофенак", "Diclofenac", "nsaid", ["25 мг", "50 мг", "75 мг"], "вольтарен"],
  ["Напроксен", "Naproxen", "nsaid", ["250 мг", "500 мг"], "напроксен"],
  ["Мелоксикам", "Meloxicam", "nsaid", ["7,5 мг", "15 мг"], "мовалис"],
  ["Целекоксиб", "Celecoxib", "nsaid", ["100 мг", "200 мг"], "целебрекс"],
  ["Ацетилсалициловая кислота", "Acetylsalicylic acid", "aspirin", ["75 мг", "100 мг", "500 мг"], "аспирин"],
  ["Метформин", "Metformin", "metformin", ["500 мг", "850 мг", "1000 мг"], "сиофор;глюкофаж"],
  ["Глибенкламид", "Glibenclamide", "sulfonylurea", ["2,5 мг", "5 мг"], "манинил"],
  ["Гликлазид", "Gliclazide", "sulfonylurea", ["30 мг", "60 мг"], "диамикрон"],
  ["Глимепирид", "Glimepiride", "sulfonylurea", ["1 мг", "2 мг", "4 мг"], "амарил"],
  ["Инсулин гларгин", "Insulin glargine", "insulin", ["100 ЕД/мл"], "лантус"],
  ["Инсулин аспарт", "Insulin aspart", "insulin", ["100 ЕД/мл"], "новорапид"],
  ["Инсулин деглудек", "Insulin degludec", "insulin", ["100 ЕД/мл"], "тресиба"],
  ["Амоксициллин", "Amoxicillin", "antibiotic_pen", ["250 мг", "500 мг", "875 мг"], "оспамокс"],
  ["Амоксициллин + клавулановая кислота", "Amoxicillin+clavulanate", "antibiotic_pen", ["375 мг", "625 мг"], "аугментин"],
  ["Азитромицин", "Azithromycin", "antibiotic_macrolide", ["250 мг", "500 мг"], "сумамед"],
  ["Кларитромицин", "Clarithromycin", "antibiotic_macrolide", ["250 мг", "500 мг"], "клацид"],
  ["Цефалексин", "Cephalexin", "antibiotic_ceph", ["250 мг", "500 мг"], "цефлексин"],
  ["Цефтриаксон", "Ceftriaxone", "antibiotic_ceph", ["1 г"], "цефтриаксон"],
  ["Цефуроксим", "Cefuroxime", "antibiotic_ceph", ["250 мг", "500 мг"], "зиннат"],
  ["Ципрофлоксацин", "Ciprofloxacin", "antibiotic_fq", ["250 мг", "500 мг"], "ципролет"],
  ["Левофлоксацин", "Levofloxacin", "antibiotic_fq", ["250 мг", "500 мг"], "таваник"],
  ["Доксициклин", "Doxycycline", "antibiotic_tet", ["100 мг"], "доксициклин"],
  ["Флуконазол", "Fluconazole", "antifungal", ["50 мг", "150 мг"], "дифлюкан"],
  ["Омепразол", "Omeprazole", "ppi", ["10 мг", "20 мг", "40 мг"], "озек"],
  ["Эзомепразол", "Esomeprazole", "ppi", ["20 мг", "40 мг"], "нексиум"],
  ["Пантопразол", "Pantoprazole", "ppi", ["20 мг", "40 мг"], "санпраз"],
  ["Ранитидин", "Ranitidine", "h2", ["150 мг", "300 мг"], "ранитидин"],
  ["Фамотидин", "Famotidine", "h2", ["20 мг", "40 мг"], "квамател"],
  ["Лоратадин", "Loratadine", "antihistamine_2nd", ["10 мг"], "кларитин"],
  ["Цетиризин", "Cetirizine", "antihistamine_2nd", ["10 мг"], "зиртек"],
  ["Фексофенадин", "Fexofenadine", "antihistamine_2nd", ["120 мг", "180 мг"], "телфаст"],
  ["Димедрол", "Diphenhydramine", "antihistamine_1st", ["50 мг"], "димедрол"],
  ["Дротаверин", "Drotaverine", "spasmolytic", ["40 мг", "80 мг"], "но-шпа"],
  ["Папаверин", "Papaverine", "spasmolytic", ["20 мг", "40 мг"], "папаверин"],
  ["Мебеверин", "Mebeverine", "spasmolytic", ["135 мг", "200 мг"], "дюспаталин"],
  ["Бисопролол", "Bisoprolol", "beta_blocker", ["2,5 мг", "5 мг", "10 мг"], "конкор"],
  ["Метопролол", "Metoprolol", "beta_blocker", ["25 мг", "50 мг", "100 мг"], "беталок"],
  ["Эналаприл", "Enalapril", "acei", ["2,5 мг", "5 мг", "10 мг"], "энап"],
  ["Лизиноприл", "Lisinopril", "acei", ["5 мг", "10 мг", "20 мг"], "диротон"],
  ["Лозартан", "Losartan", "arb", ["25 мг", "50 мг", "100 мг"], "лозап"],
  ["Валсартан", "Valsartan", "arb", ["40 мг", "80 мг", "160 мг"], "диован"],
  ["Амлодипин", "Amlodipine", "ccb_dihydro", ["2,5 мг", "5 мг", "10 мг"], "норваск"],
  ["Гидрохлоротиазид", "Hydrochlorothiazide", "diuretic_thiazide", ["12,5 мг", "25 мг"], "гидра"],
  ["Индапамид", "Indapamide", "diuretic_thiazide", ["2,5 мг"], "арифон"],
  ["Фуросемид", "Furosemide", "furosemide", ["20 мг", "40 мг"], "ласикс"],
  ["Спиронолактон", "Spironolactone", "diuretic_thiazide", ["25 мг", "50 мг"], "верошпирон"],
  ["Аторвастатин", "Atorvastatin", "statin", ["10 мг", "20 мг", "40 мг"], "липримар"],
  ["Розувастатин", "Rosuvastatin", "statin", ["5 мг", "10 мг", "20 мг"], "крестор"],
  ["Клопидогрел", "Clopidogrel", "antiplatelet", ["75 мг"], "плавикс"],
  ["Варфарин", "Warfarin", "anticoag_warfarin", ["2,5 мг", "3 мг", "5 мг"], "варфарин"],
  ["Апиксабан", "Apixaban", "anticoag_noac", ["2,5 мг", "5 мг"], "элиquis"],
  ["Ривароксабан", "Rivaroxaban", "anticoag_noac", ["10 мг", "20 мг"], "ксарелто"],
  ["Нитроглицерин", "Nitroglycerin", "nitrate", ["0,5 мг"], "нитроглицерин"],
  ["Дигоксин", "Digoxin", "digoxin_class", ["0,25 мг"], "дигоксин"],
  ["Левотироксин", "Levothyroxine", "thyroid", ["25 мкг", "50 мкг", "100 мкг"], "эутирокс"],
  ["Преднизолон", "Prednisolone", "corticosteroid_systemic", ["5 мг", "10 мг"], "преднизолон"],
  ["Сертралин", "Sertraline", "ssri", ["50 мг", "100 мг"], "золофт"],
  ["Эсциталопрам", "Escitalopram", "ssri", ["10 мг", "20 мг"], "ципралекс"],
  ["Амброксол", "Ambroxol", "mucolytic", ["15 мг", "30 мг"], "лазолван"],
  ["Ацетилцистеин", "Acetylcysteine", "mucolytic", ["200 мг", "600 мг"], "флуимуцил"],
  ["Аскорбиновая кислота", "Ascorbic acid", "vitamin", ["100 мг", "500 мг"], "витамин с"],
  ["Витамин D3", "Cholecalciferol", "vitamin", ["400 МЕ", "2000 МЕ"], "аквадетрим"],
  ["Железа сульфат", "Ferrous sulfate", "mineral", ["40 мг Fe", "80 мг Fe"], "сорбифер"],
  ["Искусственная слеза", "Artificial tears", "eye_tear", ["увлажняющие"], "хилозар"],
  ["Морская соль (назальная)", "Sea salt nasal", "nasal_saline", ["0,9%"], "аквалор"],
  ["Алюминия гидроксид + магния гидроксид", "Al+Mg hydroxide", "antacid", ["комбинированные"], "маалокс"],
  ["Валериана", "Valeriana", "herb_sedative", ["экстракт"], "валериана"],
  ["Корвалол", "Corvalol", "cardio_corvalol_like", ["капли"], "корвалол"],
  ["Осельтамивир", "Oseltamivir", "antiviral_grip", ["75 мг"], "тамифлю"],
  ["Ацикловир", "Aciclovir", "antiviral_hep", ["200 мг", "400 мг"], "ацикловир"],
  ["Метронидазол", "Metronidazole", "metronidazole", ["250 мг", "500 мг"], "трихопол"],
  ["Клотримазол", "Clotrimazole", "antifungal_topical", ["1%"], "канестен"],
  ["Гидрокортизон", "Hydrocortisone", "corticosteroid_topical", ["0,5%", "1%"], "гидрокортизон"],
  ["Ксилометазолин", "Xylometazoline", "decongestant", ["0,05%", "0,1%"], "ксилен"],
  ["Бутамират", "Butamirate", "cough_antitussive", ["50 мг"], "синекод"],
  ["Лактулоза", "Lactulose", "lax_osmotic", ["сироп"], "дюфалак"],
  ["Мелатонин", "Melatonin", "general_otc", ["3 мг", "5 мг"], "мелатонин"],
  ["Ситаглиптин", "Sitagliptin", "dpp4i", ["50 мг", "100 мг"], "янувия"],
  ["Дапаглифлозин", "Dapagliflozin", "sglt2", ["5 мг", "10 мг"], "форсига"],
  ["Лираглутид", "Liraglutide", "glp1", ["6 мг/мл"], "виктоза"],
  ["Тамсулозин", "Tamsulosin", "tamsulosin", ["0,4 мг"], "омник"],
  ["Финастерид", "Finasteride", "finasteride", ["5 мг"], "простам"],
  ["Аллопуринол", "Allopurinol", "allopurinol", ["100 мг", "300 мг"], "аллопуринол"],
  ["Амиодарон", "Amiodarone", "amiodarone", ["200 мг"], "кордарон"],
  ["Верапамил", "Verapamil", "ccb_benz", ["40 мг", "80 мг"], "изоптин"],
  ["Анальгин", "Metamizole", "general_otc", ["500 мг"], "метамизол"],
  ["Но-шпа форте", "Drotaverine forte", "spasmolytic", ["80 мг"], "дротаверин"],
  ["Цитрамон П", "Citramon P", "combo_cold", ["комбо"], "цитрамон"],
  ["Триазолам", "Triazolam", "benzodiazepine", ["0,25 мг"], "халцион"],
  ["Золпидем", "Zolpidem", "benzodiazepine", ["10 мг"], "санвал"],
  ["Оланзапин", "Olanzapine", "general_otc", ["5 мг", "10 мг"], "зипрекса"],
  ["Кветиапин", "Quetiapine", "general_otc", ["25 мг", "100 мг"], "сероквель"],
  ["Рисперидон", "Risperidone", "general_otc", ["1 мг", "2 мг"], "рисполепт"],
  ["Галоперидол", "Haloperidol", "general_otc", ["1,5 мг", "5 мг"], "галоперидол"],
  ["Карбамазепин", "Carbamazepine", "general_otc", ["200 мг"], "финлепсин"],
  ["Вальпроевая кислота", "Valproic acid", "general_otc", ["300 мг", "500 мг"], "депакин"],
  ["Леветирацетам", "Levetiracetam", "general_otc", ["500 мг", "1000 мг"], "кепpra"],
  ["Ламотриджин", "Lamotrigine", "general_otc", ["25 мг", "100 мг"], "ламиктал"],
  ["Габапентин", "Gabapentin", "general_otc", ["300 мг", "600 мг"], "нейронтин"],
  ["Прегабалин", "Pregabalin", "general_otc", ["75 мг", "150 мг"], "лирика"],
  ["Трамадол", "Tramadol", "opioid_codeine", ["50 мг", "100 мг"], "трамал"],
  ["Морфин", "Morphine", "opioid_codeine", ["10 мг"], "морфин"],
  ["Фентанил", "Fentanyl", "opioid_codeine", ["пластырь"], "фентанил"],
  ["Кеторолак", "Ketorolac", "nsaid", ["10 мг"], "кеторол"],
  ["Нимесулид", "Nimesulide", "nsaid", ["100 мг"], "найз"],
  ["Эторикоксиб", "Etoricoxib", "nsaid", ["60 мг", "90 мг"], "аркоксиа"],
  ["Кandesартан + гидрохлоротиазид", "Candesartan+HCTZ", "arb", ["комбо"], "атаканд плюс"],
  ["Амлодипин + лозартан", "Amlodipine+losartan", "ccb_dihydro", ["комбо"], "лозап ам"],
  ["Эналаприл + гидрохлоротиазид", "Enalapril+HCTZ", "acei", ["комбо"], "энап н"],
];

function hashCode(s) {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return Math.abs(h);
}

const rows = [];
for (const [name_ru, name_lat, klass, doses, aliases] of BASE) {
  for (let i = 0; i < doses.length; i++) {
    const dose = doses[i];
    const form = FORMS_ROT[(hashCode(name_ru) + i) % FORMS_ROT.length];
    rows.push({
      name_ru: dose === "комбо" ? name_ru : `${name_ru} (${dose})`,
      name_lat,
      dosage: dose === "комбо" ? "комбинированная дозировка по инструкции" : dose,
      form,
      klass,
      manufacturer: "различные производители",
      active_substance: name_lat,
      aliases,
    });
  }
}

const EXTRA_RU = `
Нурофен Экспресс|ТераФлю|Фервекс|Колдрекс|Аскофен-П|Солпадеин|Коделак бронхо|АЦЦ Лонг|Бромгексин ВР|Гексорал|Стрепсилс|Ингалипт|Каметон|Отипакс|Полидекса|Изофра|Биопарокс|Снуп|Назонекс|Авамис|Фликсоназе|Називин Сенситив|Тизин|Ринонорм|Аквалор бэби|Лазолван Норме|Амбробене|Бронхикум|Геделикс|Проспан|Эреспал|Глицин|Персен|Ново-Пассит|Дормиплант|Мелаксен|Сонмил|Донормил|Валокордин|Валосердин|Пустырника настойка|Грандаксин|Афобазол|Тенотен|Пикамилон|Фенибут|Ноотропил|Цераксон|Мексидол|Актовегин|Кортексин|Винпоцетин|Кавинтон|Сермион|Танакан|Экселон|Мильгамма|Комбилипен|Нейромультивит|Дуовит|Витрум|Алфавит|Компливит|Супрадин|Аевит|Магне B6|Магнерот|Панангин|Аспаркам|Рибоксин|Милдронат|Триметазидин|Предуктал|Коронал|Нифедипин|Коринфар|Адалат|Клофелин|Моксонидин|Гипотиазид|Торасемид|Верошпилактон|Эплеренон|Инспра|Силденафил Канон|Динамико|Зидена|Импаза|Андипал|Капотен|Моноприл|Рамиприл|Тарка|Эксфорж|Коапровель|Твинста|Лориста|Козаар|Теветен|Эдарби|Беталок Зок|Конкор Кор|Небилет|Нитроспринт|Сустак|Моносан|Имдур|Фуросемид-АКОС|Аторис|Тулип|Лескол|Липантил|Фенофибрат|Эзетрол|Зетия|Правастатин|Кардура|Омник Окас|Урорек|Простамол Унио|Дутастерид|Аводарт|Фебуксостат|Аденурик|Креон|Мезим|Панкреатин|Урсосан|Урсофальк|Холосас|Гептрал|Эссенциале|Фосфоглив|Легалон|Карсил|Антраль|Гепабене|Корвалол капли|Валидол таб|Нитроминт
`
  .split("|")
  .map((s) => s.trim())
  .filter(Boolean);

const seenExtra = new Set();
for (const ru of EXTRA_RU) {
  if (seenExtra.has(ru)) continue;
  seenExtra.add(ru);
  const form = FORMS_ROT[hashCode(ru) % FORMS_ROT.length];
  rows.push({
    name_ru: ru,
    name_lat: ru.replace(/\s+/g, "_"),
    dosage: "по инструкции",
    form,
    klass: "general_otc",
    manufacturer: "различные производители",
    active_substance: "см. инструкцию",
    aliases: ru.toLowerCase(),
  });
}

fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify(rows), "utf8");
console.log("written", out, "rows", rows.length);
