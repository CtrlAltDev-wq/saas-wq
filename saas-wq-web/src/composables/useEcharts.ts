import { onBeforeUnmount, onMounted, shallowRef, type Ref, type ShallowRef } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

export interface EchartsHandle {
  chart: ShallowRef<echarts.ECharts | undefined>
  setOption: (option: EChartsOption) => void
  resize: () => void
}

/**
 * Binds an ECharts instance to a template ref, keeps it sized while the layout
 * changes and disposes it when the page leaves. Options applied before the
 * instance exists are queued and flushed on mount.
 */
export function useEcharts(target: Ref<HTMLElement | null>): EchartsHandle {
  const chart = shallowRef<echarts.ECharts>()

  let observer: ResizeObserver | undefined

  let pending: EChartsOption | null = null

  function apply(option: EChartsOption): void {
    chart.value?.setOption(option, true)
  }

  function setOption(option: EChartsOption): void {
    if (chart.value) {
      apply(option)
      return
    }
    pending = option
  }

  function resize(): void {
    chart.value?.resize()
  }

  onMounted(() => {
    if (!target.value) {
      return
    }
    chart.value = echarts.init(target.value)
    if (pending) {
      apply(pending)
      pending = null
    }
    observer = new ResizeObserver(() => chart.value?.resize())
    observer.observe(target.value)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    chart.value?.dispose()
    chart.value = undefined
  })

  return { chart, setOption, resize }
}
