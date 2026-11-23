# 身份证行政区划码

身份证按六位县级行政区划码、有效出生日期、非 000 顺序码及 GB 11643 校验码校验。
出生日期以北京时间当天为准，按确认的业务规则要求未满 60 周岁，不限制性别。
退休边界按出生日期加 60 年处理（闰日遇非闰年取 2 月 28 日）。

区划码是以下公开码表的县级代码并集，包含撤并前的历史代码，避免将仍有效的旧身份证误判：

- 历史 GB/T 2260 码表：https://github.com/mc-zone/IDValidator/blob/master/src/GB2260.js
- 现行县级码表：https://github.com/modood/Administrative-divisions-of-China/blob/master/dist/areas.json

提取日期：2026-11-23。仅保留六位数字代码，排除省市级及“市辖区”汇总项。
现行码表来源项目使用 WTFPL，许可全文见 `identity-region-codes-LICENSE.txt`。
这属于本地结构校验，不核验姓名与证件是否匹配，也不调用公安实名接口。

前后端码表分别为 `pandora-web/src/utils/identityRegionCodes.js` 和
`pandora-backend/src/main/resources/identity/region-codes.txt`；自动测试校验两份码表一致。
更新时应保留已有历史县级代码，同时合并新县级代码。
