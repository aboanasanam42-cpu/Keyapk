#!/usr/bin/env bash
# ==============================================================================
# سكربت الرفع السريع لمشروع "الخزنة السرية" إلى مستودع GitHub الخاص بك
# Quick Push Script for Secret Vault Android Project to GitHub (Keyapk)
# ==============================================================================

set -e

REPO_URL="https://github.com/aboanasanam42-cpu/Keyapk.git"

echo "========================================================"
echo "  🌾 جاري تجهيز ورفع مشروع الخزنة السرية إلى Keyapk...  "
echo "  المستودع: $REPO_URL"
echo "========================================================"

# 1. التأكد من وجود Git أو تهيئته
if [ ! -d ".git" ]; then
    echo ">> [1/4] تهيئة مستودع Git محلياً..."
    git init
    git branch -M main
else
    echo ">> [1/4] مستودع Git مهيأ مسبقاً."
fi

# 2. إعطاء صلاحية التنفيذ للـ Gradle Wrapper
if [ -f "gradlew" ]; then
    chmod +x gradlew
    git update-index --chmod=+x gradlew 2>/dev/null || true
fi

# 3. إعداد المستودع البعيد (Remote Origin)
EXISTING_REMOTE=$(git remote get-url origin 2>/dev/null || true)
if [ -z "$EXISTING_REMOTE" ]; then
    echo ">> [2/4] ربط المستودع البعيد بـ: $REPO_URL"
    git remote add origin "$REPO_URL"
else
    if [ "$EXISTING_REMOTE" != "$REPO_URL" ]; then
        echo ">> [2/4] تحديث رابط المستودع البعيد إلى: $REPO_URL"
        git remote set-url origin "$REPO_URL"
    else
        echo ">> [2/4] المستودع البعيد مرتبط مسبقاً بالشكل الصحيح."
    fi
fi

# 4. إضافة جميع الملفات وعمل Commit
echo ">> [3/4] تجهيز الملفات وحفظ الالتزام (Commit)..."
git add .
git commit -m "feat: complete Secret Vault Android app with CI/CD and custom wheat ear icon" || echo "لا توجد تعديلات جديدة للالتزام بها."

# 5. الدفع إلى GitHub
echo ">> [4/4] دفع المشروع إلى GitHub (main branch)..."
git push -u origin main

echo "========================================================"
echo "✅ تم رفع جميع الملفات بنجاح إلى مستودع Keyapk!"
echo "سيبدأ الآن GitHub Actions فوراً ببناء ملف APK الموقع:"
echo "https://github.com/aboanasanam42-cpu/Keyapk/actions"
echo "========================================================"
