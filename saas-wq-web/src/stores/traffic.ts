import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const BUCKET_MS = 60_000

const WINDOW_MINUTES = 30

/**
 * Counts the API calls issued by this browser and keeps the last 30 one minute
 * buckets. It gives the dashboard a truthful traffic signal without inventing
 * server side metrics the backend does not expose yet.
 */
export const useTrafficStore = defineStore('traffic', () => {
  const buckets = ref<Record<number, number>>({})

  const total = ref(0)

  /** Incremented by the dashboard clock so the window slides even when idle. */
  const tick = ref(0)

  function track(): void {
    const bucket = Math.floor(Date.now() / BUCKET_MS)
    buckets.value = { ...buckets.value, [bucket]: (buckets.value[bucket] ?? 0) + 1 }
    total.value += 1
    prune()
  }

  function prune(): void {
    const oldest = Math.floor(Date.now() / BUCKET_MS) - WINDOW_MINUTES + 1
    Object.keys(buckets.value).forEach((key) => {
      if (Number(key) < oldest) {
        delete buckets.value[Number(key)]
      }
    })
  }

  function pulse(): void {
    tick.value += 1
  }

  const series = computed(() => {
    // Reading the tick keeps the computed reactive while the window slides.
    void tick.value
    const newest = Math.floor(Date.now() / BUCKET_MS)
    const labels: string[] = []
    const values: number[] = []
    for (let offset = WINDOW_MINUTES - 1; offset >= 0; offset -= 1) {
      const bucket = newest - offset
      const clock = new Date(bucket * BUCKET_MS)
      labels.push(
        `${String(clock.getHours()).padStart(2, '0')}:${String(clock.getMinutes()).padStart(2, '0')}`
      )
      values.push(buckets.value[bucket] ?? 0)
    }
    return { labels, values }
  })

  return { buckets, total, tick, track, pulse, series }
})
