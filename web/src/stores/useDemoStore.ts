import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDemoStore = defineStore('demoStore', () => {
  const hel = ref('nb')

  const setHel = (value: string) => {
    hel.value = value
  }

  return {
    hel,
    setHel
  }
})
