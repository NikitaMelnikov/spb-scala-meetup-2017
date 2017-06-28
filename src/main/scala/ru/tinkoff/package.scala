package ru

package object tinkoff {
  sealed trait ApplicationMode
  object ApplicationModes {
    case object Artery extends ApplicationMode
    case object Netty extends ApplicationMode
  }

  sealed trait ApplicationRole
  object ApplicationRoles {
    case object Publisher extends ApplicationRole
    case object Subscriber extends ApplicationRole
  }
}
