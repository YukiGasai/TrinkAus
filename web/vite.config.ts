import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// @ts-expect-error (importing a non-TS module)
import signals from '@preact/signals-react-transform';
import svgr from 'vite-plugin-svgr'

export default defineConfig({
    base: '/',
    plugins: [svgr(), react({
        babel: {
            plugins: [signals],
        },
    })],
})
