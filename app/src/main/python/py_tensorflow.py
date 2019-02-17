import tensorflow as tf

def tfTest():
    node1 = tf.constant(3.0, tf.float32)
    node2 = tf.constant(4.0)
    print(node1, node2)
    sess = tf.Session()
    print(sess.run([node1, node2]))