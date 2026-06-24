# Контекст проекта: Minecraft Visuals (Fabric 1.20+ / Yarn-Mojang)
Мы пишем кастомные визуальные модули. Стек использует Lombok (`@FieldDefaults`), ивенты `@EventHandler` и современные обертки рендеринга Minecraft.

## Архитектурные правила для ИИ

### 1. Плавность рендеринга (Интерполяция partialTicks)
- Никогда не используй «чистые» координаты из `WorldRenderEvent` без учета `e.getTickDelta()` (или `partialTicks`).
- Позиция камеры `camera.getPos()` уже интерполирована игрой. Но если мы берем статичные координаты из `TickEvent` (`mc.player.getX()`), при рендере возникнет «желе» и тряска.
- Для кругов: время `circle.timer.getPassedTimeMs()` в методе рендера нужно считать динамически, чтобы анимация расширения scale была плавной (завязана на кадры FPS, а не на тики сервера).

### 2. Матрицы и Трансляция (MatrixStack)
- При работе с `WorldRenderEvent` НЕ нужно вручную крутить матрицу на Pitch/Yaw камеры для выравнивания к миру, если ты рендеришь в мировых координатах.
- Правильный порядок трансляции относительно камеры:
  `matrixStack.translate(circlePos.x - cameraPos.x, circlePos.y - cameraPos.y, circlePos.z - cameraPos.z);`
- Чтобы положить круг горизонтально на землю: достаточно повернуть его один раз по оси X на 90 градусов после трансляции: `matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));`

## Утилиты проекта
- Текстуры: `Render3DUtil.drawTexture(MatrixStack.Entry, Identifier, x, y, width, height, Vector4i colors, boolean blend)`
- Цвета: `ColorUtil.fade(int offset)`, `ColorUtil.multAlpha(int color, float alpha)`

