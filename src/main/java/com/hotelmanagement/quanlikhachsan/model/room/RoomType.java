package com.hotelmanagement.quanlikhachsan.model.room;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Table(name = "room_types")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    // @OneToMany(mappedBy = "roomType")
    // @Builder.Default
    // private List<Room> rooms = new ArrayList<>();
}
