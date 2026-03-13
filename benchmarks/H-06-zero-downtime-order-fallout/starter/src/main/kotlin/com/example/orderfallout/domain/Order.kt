package com.example.orderfallout.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "customer_name", nullable = false)
    var customerName: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.DRAFT,

    @Column(name = "delivery_address", nullable = false)
    var deliveryAddress: String = "",

    @Column(name = "customer_reference")
    var customerReference: String? = null,

    @Column(name = "notes")
    var notes: String? = null,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lines: MutableList<OrderLine> = mutableListOf()
) {

    fun addLine(line: OrderLine) {
        line.order = this
        lines += line
    }
}
