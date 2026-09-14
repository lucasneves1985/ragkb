import type { Component } from 'vue'

export interface NavItem {
  label: string
  to: string
  icon: Component
  visible: boolean
}
