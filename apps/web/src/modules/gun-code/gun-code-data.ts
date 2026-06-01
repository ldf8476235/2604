export type GunCodeCategory =
  | "冲锋枪"
  | "手枪"
  | "步枪"
  | "特殊武器"
  | "狙击步枪"
  | "精确射手步枪"
  | "轻机枪"
  | "霰弹枪"
  | "主播顶护魔王同款";

export type GunCodeVoteType = "LIKE" | "DISLIKE";

export type GunCodeEntry = {
  code: string;
  title: string;
  category: GunCodeCategory;
  likes: number;
  dislikes: number;
  tags?: string[];
  currentVote?: GunCodeVoteType | null;
};

export type GunCodeGroup = {
  id: string;
  creator: string;
  source: string;
  badges: string[];
  entries: GunCodeEntry[];
};

export const gunCodeCategoryOptions: GunCodeCategory[] = [
  "冲锋枪",
  "手枪",
  "步枪",
  "特殊武器",
  "狙击步枪",
  "精确射手步枪",
  "轻机枪",
  "霰弹枪",
  "主播顶护魔王同款",
];

export const gunCodeTagOptions = ["全部标签", "主播同款", "版本强势T0", "修脚", "主播顶护魔王同款"] as const;

export const gunCodeGroups: GunCodeGroup[] = [
  {
    id: "bosh",
    source: "抖音",
    creator: "Bosh",
    badges: ["主播顶护魔王同款"],
    entries: [
      { code: "6J4F3B001T61VNNP5VK98", title: "M14射手步枪", category: "精确射手步枪", likes: 6, dislikes: 57 },
      { code: "6J1V9N80BVIRVL16CV1AB", title: "M700狙击步枪", category: "狙击步枪", likes: 5, dislikes: 27 },
      { code: "6JEIQE800M7RFBSRGE5GE", title: "M14射手步枪", category: "精确射手步枪", likes: 4, dislikes: 31 },
      { code: "613FQ880CVVHEAFPPG8CH", title: "M250通用机枪", category: "轻机枪", likes: 3, dislikes: 27 },
      { code: "6J8KCPC02D5DHK6SCDPOP", title: "SR-25射手步枪", category: "精确射手步枪", likes: 2, dislikes: 29 },
    ],
  },
  {
    id: "chena",
    source: "抖音",
    creator: "辰阿",
    badges: ["主播顶护魔王同款", "主播同款"],
    entries: [
      { code: "6JIHRKC0473LGNSN240H5", title: "AS Val突击步枪", category: "步枪", likes: 6, dislikes: 35, tags: ["主播同款"] },
      { code: "6JIHRQG0473LGNSN240H5", title: "AS Val突击步枪 斜握腰射61", category: "步枪", likes: 3, dislikes: 22, tags: ["主播同款"] },
      { code: "6JIHS4S0473LGNSN240H5", title: "M7战斗步枪 3.5倍", category: "步枪", likes: 3, dislikes: 16 },
      { code: "6JIHSAK0473LGNSN240H5", title: "M7战斗步枪 红点", category: "步枪", likes: 2, dislikes: 17 },
      { code: "6JIHRUO0473LGNSN240H5", title: "SR-25射手步枪", category: "精确射手步枪", likes: 1, dislikes: 18 },
    ],
  },
  {
    id: "hebeilong",
    source: "抖音",
    creator: "河北龙",
    badges: ["主播顶护魔王同款", "主播同款"],
    entries: [
      { code: "6JBT1S804RKG5PEP9BL67", title: "AS Val突击步枪", category: "步枪", likes: 2, dislikes: 12, tags: ["主播同款"] },
      { code: "6JGLRIO094JVITT8VSU6I", title: "AS Val突击步枪", category: "步枪", likes: 0, dislikes: 11, tags: ["主播同款"] },
      { code: "6JCVNS807MAMKEF8I6OO5", title: "AS Val突击步枪", category: "步枪", likes: 0, dislikes: 11 },
      { code: "6JCB5I009S4O9TFSL4USO", title: "AS Val突击步枪", category: "步枪", likes: 0, dislikes: 11 },
      { code: "6J2296O05VO2TE43IMV7H", title: "M14射手步枪", category: "精确射手步枪", likes: 0, dislikes: 10 },
    ],
  },
  {
    id: "shu",
    source: "抖音",
    creator: "赎",
    badges: ["主播顶护魔王同款", "主播同款"],
    entries: [
      { code: "6JIEOS404A5GIV02K4A4U", title: "M7战斗步枪", category: "步枪", likes: 3, dislikes: 17, tags: ["主播同款"] },
      { code: "6JIEO1C04A5GIV02K4A4U", title: "AS Val突击步枪", category: "步枪", likes: 1, dislikes: 18 },
      { code: "6JIEO5C04A5GIV02K4A4U", title: "AWM狙击步枪", category: "狙击步枪", likes: 1, dislikes: 16 },
      { code: "6JIEOBK04A5GIV02K4A4U", title: "M14射手步枪", category: "精确射手步枪", likes: 1, dislikes: 16 },
    ],
  },
  {
    id: "hk1ng",
    source: "抖音",
    creator: "Hk1ng",
    badges: ["主播顶护魔王同款", "主播同款"],
    entries: [
      { code: "6JA0LF804LEDIEE22K4SR", title: "M14射手步枪", category: "精确射手步枪", likes: 2, dislikes: 13 },
      { code: "6JA0LHO04LEDIEE22K4SR", title: "M7战斗步枪", category: "步枪", likes: 1, dislikes: 11 },
      { code: "6JA0LJC04LEDIEE22K4SR", title: "M700狙击步枪", category: "狙击步枪", likes: 1, dislikes: 12 },
      { code: "6JA0MCO04LEDIEE22K4SR", title: "SR-3M紧凑突击步枪", category: "步枪", likes: 1, dislikes: 10 },
      { code: "6JA0MU004LEDIEE22K4SR", title: "腾龙突击步枪", category: "步枪", likes: 1, dislikes: 10 },
    ],
  },
  {
    id: "wukai",
    source: "抖音",
    creator: "悟凯",
    badges: ["主播顶护魔王同款", "主播同款"],
    entries: [
      { code: "6JALDNS096GBR33JHF31J", title: "MK47突击步枪", category: "步枪", likes: 0, dislikes: 12, tags: ["主播同款"] },
      { code: "6JALDS0096GBR33JHF31J", title: "AS Val突击步枪", category: "步枪", likes: 0, dislikes: 9 },
      { code: "6JALE08096GBR33JHF31J", title: "M14射手步枪", category: "精确射手步枪", likes: 0, dislikes: 9 },
      { code: "6JALEBS096GBR33JHF31J", title: "M7战斗步枪", category: "步枪", likes: 0, dislikes: 9 },
      { code: "6JALEGS096GBR33JHF31J", title: "M7战斗步枪", category: "步枪", likes: 0, dislikes: 9 },
    ],
  },
  {
    id: "s12k",
    source: "",
    creator: "S12K",
    badges: ["霰弹枪"],
    entries: [
      { code: "6JDANNK0B1H3TI624U64G", title: "14w丐版", category: "霰弹枪", likes: 4, dislikes: 8 },
      { code: "6JDANPO0B1H3TI624U64G", title: "23w满改", category: "霰弹枪", likes: 2, dislikes: 7 },
    ],
  },
  {
    id: "fs12",
    source: "",
    creator: "FS-12霰弹枪",
    badges: ["霰弹枪", "修脚", "版本强势T0"],
    entries: [
      { code: "6JFFIU804U3TN9SQ0V71K", title: "29w手枪版", category: "霰弹枪", likes: 4, dislikes: 6, tags: ["修脚", "版本强势T0"] },
      { code: "6JFFIV804U3TN9SQ0V71K", title: "37W主枪版", category: "霰弹枪", likes: 2, dislikes: 7, tags: ["修脚"] },
    ],
  },
  {
    id: "shotgun725",
    source: "",
    creator: "725双管霰弹枪",
    badges: ["霰弹枪"],
    entries: [
      { code: "6JCBPEG01OMBBV761REOP", title: "25W莽侠满改", category: "霰弹枪", likes: 1, dislikes: 4 },
    ],
  },
];
