package com.hotelmanagement.quanlikhachsan.model.room;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "rooms")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "room_number")
    private String roomNumber;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType type;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_status_id", nullable = false)
    private RoomStatus status;

    private short floor;

    private String note;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoomImages> images = new ArrayList<>();

}
