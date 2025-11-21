package com.hotelmanagement.quanlikhachsan.model.room;


import jakarta.persistence.*;
import lombok.*;

@Table(name = "room_statuses")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RoomStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "name")
    private String name;
}
