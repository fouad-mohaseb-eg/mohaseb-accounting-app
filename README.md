# Mohaseb (محاسب) - simple Arabic accounting app for Android

A small, free, offline Android app for one person running a small shop. The interface is in Arabic and the currency is the Egyptian pound (EGP).

## Features

- Cash box (الخزنة)
- Profits (الأرباح), with credit sales counted at the time of sale
- Debts (الديون)
- Customers and their accounts (العملاء)
- Inventory (المخزن)
- Daily, weekly (week starts Saturday), monthly and yearly reports
- Excel export for reports and backup

## Status

Early stage (v1.5). It has not been tested widely on real devices yet. Use dummy data first. Feedback and pull requests are welcome.

## Build

The source is plain Java with no Gradle project. See `build.sh` for the Android SDK build steps.

```
./build.sh
```

## Layout

- `src/com/fouad/acc/` - activities and helpers
- `src/res/` - animations and styles
- `src/AndroidManifest.xml`

## Contributing

Open an issue or a pull request. Arabic and English are both fine.

---

# محاسب - تطبيق محاسبة بسيط للأندرويد

تطبيق مجاني يعمل بدون إنترنت لصاحب محل صغير. الواجهة بالعربي والعملة الجنيه المصري.

المميزات: الخزنة، الأرباح، الديون، العملاء وحساباتهم، المخزن، تقارير يومية وأسبوعية وشهرية وسنوية، وتصدير Excel للتقارير والنسخ الاحتياطي.

المشروع في مرحلة مبكرة. جرّبه ببيانات تجريبية أولًا. المساهمات مرحب بها.
